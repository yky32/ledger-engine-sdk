package com.altech.ledger.sdk.file;

import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.json.JsonSupport;
import com.altech.ledger.sdk.kafka.KafkaLedgerClient;
import com.altech.ledger.sdk.model.IngestionResult;
import com.altech.ledger.sdk.model.TransactionalEvent;
import com.altech.ledger.sdk.rest.RestLedgerClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Channel 3 (optional) — file-based batch of {@link TransactionalEvent}.
 * <p>
 * Supported formats:
 * <ul>
 *   <li>JSON array: {@code [ {...}, {...} ]}</li>
 *   <li>NDJSON: one event JSON object per line</li>
 * </ul>
 * Delivery: REST (default) or Kafka.
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
        this.rest = rest; // may be null for parse-only usage
        this.kafka = kafka;
    }

    /** Parse without submitting. */
    public static List<TransactionalEvent> parse(Path file) {
        return new FileLedgerClient(null, null).readEvents(file);
    }

    public List<TransactionalEvent> readEvents(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (text.isEmpty()) {
                return List.of();
            }
            if (text.startsWith("[")) {
                List<TransactionalEvent> list = mapper.readValue(text, new TypeReference<>() {});
                list.forEach(TransactionalEvent::validate);
                return list;
            }
            // NDJSON
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
                    } catch (Exception ex) {
                        throw new LedgerException("Invalid event at line " + lineNo + ": " + ex.getMessage(), ex);
                    }
                }
            }
            return events;
        } catch (LedgerException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new LedgerException("Failed to read file " + file + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Read file and submit each event.
     *
     * @return REST results (empty list if Kafka delivery)
     */
    public List<IngestionResult> process(Path file, Delivery delivery) {
        List<TransactionalEvent> events = readEvents(file);
        return submit(events, delivery);
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

    /** Peek first event shape without full validation of file size (debug). */
    public JsonNode peek(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8).trim();
        return mapper.readTree(text);
    }
}
