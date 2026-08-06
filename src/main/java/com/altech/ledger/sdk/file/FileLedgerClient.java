package com.altech.ledger.sdk.file;

import com.altech.ledger.sdk.DeliveryChannel;
import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.LedgerValidationException;
import com.altech.ledger.sdk.api.FileApi;
import com.altech.ledger.sdk.batch.BatchOptions;
import com.altech.ledger.sdk.batch.BatchResult;
import com.altech.ledger.sdk.json.JsonSupport;
import com.altech.ledger.sdk.kafka.KafkaLedgerClient;
import com.altech.ledger.sdk.model.IngestionResult;
import com.altech.ledger.sdk.model.TransactionalEvent;
import com.altech.ledger.sdk.rest.RestLedgerClient;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * Channel 3 (optional) — file-based batch of {@link TransactionalEvent}.
 * <p>
 * Prefer {@link FileApi} via {@code client.files()} for streaming + {@link BatchOptions}.
 * This class remains for 1.0-compatible list-based APIs.
 */
public final class FileLedgerClient {
    public enum Delivery { REST, KAFKA }

    private final ObjectMapper mapper = JsonSupport.mapper();
    private final RestLedgerClient rest;
    private final KafkaLedgerClient kafka;

    public FileLedgerClient(RestLedgerClient rest) {
        this(rest, null);
    }

    public FileLedgerClient(RestLedgerClient rest, KafkaLedgerClient kafka) {
        this.rest = rest;
        this.kafka = kafka;
    }

    /** Parse without submitting. */
    public static List<TransactionalEvent> parse(Path file) {
        return new FileLedgerClient(null, null).readEvents(file);
    }

    /**
     * Read all events into memory. For large NDJSON files prefer {@link FileApi#process}.
     */
    public List<TransactionalEvent> readEvents(Path file) {
        try {
            if (!Files.exists(file)) {
                throw new LedgerException("File not found: " + file);
            }
            // Stream-parse into list (array or NDJSON) without Files.readString for large files
            Format format = detectFormat(file);
            return switch (format) {
                case EMPTY -> List.of();
                case NDJSON -> readNdjson(file);
                case JSON_ARRAY -> readJsonArray(file);
            };
        } catch (LedgerException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new LedgerException("Failed to read file " + file + ": " + ex.getMessage(), ex);
        }
    }

    private List<TransactionalEvent> readNdjson(Path file) throws IOException {
        List<TransactionalEvent> events = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    TransactionalEvent e = mapper.readValue(line, TransactionalEvent.class);
                    e.validate();
                    events.add(e);
                } catch (LedgerException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new LedgerValidationException(
                        "Invalid event at line " + lineNo + ": " + ex.getMessage(), ex);
                }
            }
        }
        return events;
    }

    private List<TransactionalEvent> readJsonArray(Path file) throws IOException {
        List<TransactionalEvent> events = new ArrayList<>();
        try (InputStream in = Files.newInputStream(file);
             JsonParser parser = mapper.getFactory().createParser(in)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new LedgerValidationException("Expected JSON array at root of " + file);
            }
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                TransactionalEvent e = mapper.readValue(parser, TransactionalEvent.class);
                e.validate();
                events.add(e);
            }
        }
        return events;
    }

    /**
     * Read file and submit each event (fail-fast).
     *
     * @return REST results (empty list if Kafka delivery)
     */
    public List<IngestionResult> process(Path file, Delivery delivery) {
        BatchResult<IngestionResult> batch = processBatch(file, delivery, BatchOptions.failFast());
        batch.throwIfAnyFailed();
        return batch.successes();
    }

    /** Streaming process with batch options (partial results, progress). */
    public BatchResult<IngestionResult> processBatch(Path file, Delivery delivery, BatchOptions options) {
        FileApi api = new FileApi(rest, () -> {
            if (kafka == null) {
                throw new IllegalStateException("Kafka client not configured for FILE→Kafka delivery");
            }
            return kafka;
        });
        DeliveryChannel channel = delivery == Delivery.KAFKA ? DeliveryChannel.KAFKA : DeliveryChannel.REST;
        return api.process(file, channel, options);
    }

    public List<IngestionResult> submit(List<TransactionalEvent> events, Delivery delivery) {
        Objects.requireNonNull(delivery, "delivery");
        if (delivery == Delivery.KAFKA) {
            if (kafka == null) {
                throw new IllegalStateException("Kafka client not configured for FILE→Kafka delivery");
            }
            for (TransactionalEvent e : events) {
                kafka.publish(e);
            }
            kafka.flush();
            return List.of();
        }
        if (rest == null) {
            throw new IllegalStateException("REST client not configured for FILE→REST delivery");
        }
        List<IngestionResult> results = new ArrayList<>(events.size());
        for (TransactionalEvent e : events) {
            results.add(rest.ingestTransaction(e));
        }
        return results;
    }

    public JsonNode peek(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file);
             JsonParser parser = mapper.getFactory().createParser(in)) {
            return mapper.readTree(parser);
        }
    }

    private enum Format { EMPTY, NDJSON, JSON_ARRAY }

    private static Format detectFormat(Path file) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("[")) return Format.JSON_ARRAY;
                return Format.NDJSON;
            }
        }
        return Format.EMPTY;
    }
}
