package com.altech.ledger.sdk.cli;

import com.altech.ledger.sdk.LedgerClient;
import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.file.FileLedgerClient;
import com.altech.ledger.sdk.model.IngestionResult;

import java.nio.file.Path;
import java.util.List;

/**
 * Runnable JAR entrypoint for optional file-based ingestion.
 * <pre>
 * java -jar ledger-engine-sdk-0.1.0-SNAPSHOT.jar \
 *   --base-url http://localhost:8080 \
 *   --file ./events.ndjson \
 *   --delivery REST
 * </pre>
 */
public final class FileIngestCli {
    private FileIngestCli() {}

    public static void main(String[] args) {
        String baseUrl = envOr("LEDGER_BASE_URL", "http://localhost:8080");
        String file = null;
        String delivery = "REST";
        String kafkaBootstrap = envOr("KAFKA_BOOTSTRAP", null);
        String kafkaTopic = envOr("KAFKA_TOPIC", "ledger.transaction.events");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--base-url" -> baseUrl = args[++i];
                case "--file" -> file = args[++i];
                case "--delivery" -> delivery = args[++i];
                case "--kafka-bootstrap" -> kafkaBootstrap = args[++i];
                case "--kafka-topic" -> kafkaTopic = args[++i];
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
                default -> throw new IllegalArgumentException("Unknown arg: " + args[i]);
            }
        }
        if (file == null) {
            printHelp();
            System.exit(2);
        }

        LedgerClientConfig.Builder cfg = LedgerClientConfig.builder().baseUrl(baseUrl);
        if (kafkaBootstrap != null) {
            cfg.kafkaBootstrapServers(kafkaBootstrap).kafkaTopic(kafkaTopic);
        }

        FileLedgerClient.Delivery mode = FileLedgerClient.Delivery.valueOf(delivery.toUpperCase());
        try (LedgerClient client = LedgerClient.create(cfg.build())) {
            List<IngestionResult> results = client.file().process(Path.of(file), mode);
            if (mode == FileLedgerClient.Delivery.KAFKA) {
                System.out.println("Published events from " + file + " to Kafka (async).");
            } else {
                int applied = 0, skipped = 0, other = 0;
                for (IngestionResult r : results) {
                    System.out.println(r);
                    if (r.isApplied()) applied++;
                    else if (r.getStatus() == IngestionResult.Status.SKIPPED) skipped++;
                    else other++;
                }
                System.out.printf("Done. total=%d applied=%d skipped=%d other=%d%n",
                    results.size(), applied, skipped, other);
            }
        }
    }

    private static String envOr(String k, String d) {
        String v = System.getenv(k);
        return v == null || v.isBlank() ? d : v;
    }

    private static void printHelp() {
        System.out.println("""
            ledger-engine-sdk file ingest CLI

            Usage:
              java -jar ledger-engine-sdk-*.jar --file events.ndjson [--base-url URL] [--delivery REST|KAFKA]

            Options:
              --base-url URL           Ledger base URL (default http://localhost:8080)
              --file PATH              JSON array or NDJSON of TransactionalEvent
              --delivery REST|KAFKA    Default REST
              --kafka-bootstrap HOSTS  Required for KAFKA delivery
              --kafka-topic TOPIC      Default ledger.transaction.events
            """);
    }
}
