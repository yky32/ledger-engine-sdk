package com.altech.ledger.sdk.api;

import com.altech.ledger.sdk.DeliveryChannel;
import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.LedgerValidationException;
import com.altech.ledger.sdk.batch.BatchOptions;
import com.altech.ledger.sdk.batch.BatchResult;
import com.altech.ledger.sdk.batch.ItemResult;
import com.altech.ledger.sdk.file.FileLedgerClient;
import com.altech.ledger.sdk.json.JsonSupport;
import com.altech.ledger.sdk.kafka.KafkaLedgerClient;
import com.altech.ledger.sdk.model.IngestionResult;
import com.altech.ledger.sdk.model.TransactionalEvent;
import com.altech.ledger.sdk.rest.RestLedgerClient;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Resource API — file-based batch ingest with streaming NDJSON support.
 * <pre>
 * client.files().process(path);  // REST, fail-fast
 * client.files().process(path, DeliveryChannel.REST,
 *     BatchOptions.continueOnError().withProgress(ProgressListener.loggingEvery(500)));
 * </pre>
 */
public final class FileApi {
    private final RestLedgerClient rest;
    private final Supplier<KafkaLedgerClient> kafka;
    private final ObjectMapper mapper = JsonSupport.mapper();

    public FileApi(RestLedgerClient rest, Supplier<KafkaLedgerClient> kafka) {
        this.rest = rest;
        this.kafka = Objects.requireNonNull(kafka, "kafka");
    }

    /** Parse file fully into memory (small files / validation). */
    public List<TransactionalEvent> parse(Path file) {
        return FileLedgerClient.parse(file);
    }

    /** REST process, fail-fast (backward-compatible behaviour). */
    public BatchResult<IngestionResult> process(Path file) {
        return process(file, DeliveryChannel.REST, BatchOptions.failFast());
    }

    public BatchResult<IngestionResult> process(Path file, DeliveryChannel channel, BatchOptions options) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(channel, "channel");
        BatchOptions opts = options == null ? BatchOptions.defaults() : options;

        try {
            Format format = detectFormat(file);
            return switch (format) {
                case EMPTY -> {
                    BatchResult<IngestionResult> empty = BatchResult.empty();
                    opts.getProgress().onComplete(empty);
                    yield empty;
                }
                case NDJSON -> streamNdjson(file, channel, opts);
                case JSON_ARRAY -> streamJsonArray(file, channel, opts);
            };
        } catch (LedgerException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new LedgerException("Failed to process file " + file + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Stream NDJSON line-by-line without loading the whole file (preferred for 70K+).
     */
    private BatchResult<IngestionResult> streamNdjson(Path file, DeliveryChannel channel,
                                                      BatchOptions opts) throws IOException {
        List<ItemResult<IngestionResult>> items = new ArrayList<>();
        int index = 0;
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                ItemResult<IngestionResult> ir = processOne(index, lineNo, line, channel, opts);
                if (ir == null) {
                    // fail-fast already threw
                    break;
                }
                items.add(ir);
                opts.getProgress().onItem(index, -1, ir);
                if (ir.isFailure() && !opts.isContinueOnError()) {
                    break;
                }
                index++;
            }
        }
        if (channel == DeliveryChannel.KAFKA) {
            kafka.get().flush();
        }
        BatchResult<IngestionResult> batch = new BatchResult<>(items);
        opts.getProgress().onComplete(batch);
        if (!opts.isContinueOnError() && batch.hasFailures()) {
            batch.throwIfAnyFailed();
        }
        return batch;
    }

    /**
     * Stream JSON array with Jackson token parser (does not hold all events if processed one-by-one).
     */
    private BatchResult<IngestionResult> streamJsonArray(Path file, DeliveryChannel channel,
                                                         BatchOptions opts) throws IOException {
        List<ItemResult<IngestionResult>> items = new ArrayList<>();
        int index = 0;
        try (InputStream in = Files.newInputStream(file);
             JsonParser parser = mapper.getFactory().createParser(in)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new LedgerValidationException("Expected JSON array at root of " + file);
            }
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                TransactionalEvent event = mapper.readValue(parser, TransactionalEvent.class);
                ItemResult<IngestionResult> ir = processEvent(index, event, channel, opts);
                items.add(ir);
                opts.getProgress().onItem(index, -1, ir);
                if (ir.isFailure() && !opts.isContinueOnError()) {
                    BatchResult<IngestionResult> partial = new BatchResult<>(items);
                    opts.getProgress().onComplete(partial);
                    if (ir.getError() != null) {
                        throw ir.getError();
                    }
                    break;
                }
                index++;
            }
        }
        if (channel == DeliveryChannel.KAFKA) {
            kafka.get().flush();
        }
        BatchResult<IngestionResult> batch = new BatchResult<>(items);
        opts.getProgress().onComplete(batch);
        return batch;
    }

    private ItemResult<IngestionResult> processOne(int index, int lineNo, String json,
                                                   DeliveryChannel channel,
                                                   BatchOptions opts) {
        TransactionalEvent event;
        try {
            event = mapper.readValue(json, TransactionalEvent.class);
            event.validate();
        } catch (LedgerException ex) {
            ItemResult<IngestionResult> ir = ItemResult.failure(index, "line-" + lineNo, ex);
            if (!opts.isContinueOnError()) {
                throw ex;
            }
            return ir;
        } catch (Exception ex) {
            LedgerValidationException ve = new LedgerValidationException(
                "Invalid event at line " + lineNo + ": " + ex.getMessage(), ex);
            if (!opts.isContinueOnError()) {
                throw ve;
            }
            return ItemResult.failure(index, "line-" + lineNo, ve);
        }
        return processEvent(index, event, channel, opts);
    }

    private ItemResult<IngestionResult> processEvent(int index, TransactionalEvent event,
                                                     DeliveryChannel channel,
                                                     BatchOptions opts) {
        String id = event.getEventId();
        try {
            if (channel == DeliveryChannel.REST) {
                if (rest == null) {
                    throw new IllegalStateException("REST client not configured");
                }
                IngestionResult result = rest.ingestTransaction(event);
                if (result != null && (result.getStatus() == IngestionResult.Status.SKIPPED
                    || result.getStatus() == IngestionResult.Status.DUPLICATE)) {
                    return ItemResult.skipped(index, id, result);
                }
                return ItemResult.success(index, id, result);
            }
            kafka.get().publish(event);
            return ItemResult.success(index, id, null);
        } catch (LedgerException ex) {
            if (!opts.isContinueOnError()) {
                throw ex;
            }
            return ItemResult.failure(index, id, ex);
        }
    }

    private enum Format { EMPTY, NDJSON, JSON_ARRAY }

    private static Format detectFormat(Path file) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("[")) {
                    return Format.JSON_ARRAY;
                }
                return Format.NDJSON;
            }
        }
        return Format.EMPTY;
    }
}
