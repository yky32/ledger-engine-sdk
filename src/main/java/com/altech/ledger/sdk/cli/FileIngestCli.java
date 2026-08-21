package com.altech.ledger.sdk.cli;

import com.altech.ledger.sdk.DeliveryChannel;
import com.altech.ledger.sdk.LedgerClient;
import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.batch.BatchOptions;
import com.altech.ledger.sdk.batch.BatchResult;
import com.altech.ledger.sdk.batch.ProgressListener;
import com.altech.ledger.sdk.model.IngestionResult;

import java.nio.file.Path;

/**
 * Runnable CLI entrypoint for optional file-based ingestion.
 * Packaged as {@code ledger-engine-sdk-*-cli.jar} (shaded).
 * <pre>
 * java -jar ledger-engine-sdk-1.1.0-cli.jar \
 *   --base-url http://localhost:8080 \
 *   --file ./events.ndjson \
 *   --delivery REST \
 *   --continue-on-error
 * </pre>
 */
public final class FileIngestCli {
    private FileIngestCli() {}

    public static void main(String[] args) {
        String baseUrl = envOr("LEDGER_BASE_URL", "http://localhost:8080");
        String bearer = envOr("LEDGER_TOKEN", null);
        String apiKey = envOr("LEDGER_API_KEY", null);
        String file = null;
        String delivery = "REST";
        String kafkaBootstrap = envOr("KAFKA_BOOTSTRAP", null);
        String kafkaTopic = envOr("KAFKA_TOPIC", "ledger.transaction.events");
        String externalType = envOr("LEDGER_EXTERNAL_TYPE", "");
        boolean continueOnError = false;
        int progressEvery = 100;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--base-url" -> baseUrl = args[++i];
                case "--file" -> file = args[++i];
                case "--delivery" -> delivery = args[++i];
                case "--kafka-bootstrap" -> kafkaBootstrap = args[++i];
                case "--kafka-topic" -> kafkaTopic = args[++i];
                case "--token" -> bearer = args[++i];
                case "--api-key" -> apiKey = args[++i];
                case "--external-type" -> externalType = args[++i];
                case "--continue-on-error" -> continueOnError = true;
                case "--progress-every" -> progressEvery = Integer.parseInt(args[++i]);
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

        LedgerClientConfig.Builder cfg = LedgerClientConfig.builder()
            .baseUrl(baseUrl)
            .defaultExternalType(externalType)
            .defaultCurrency("LP");
        if (bearer != null) {
            cfg.bearerToken(bearer);
        }
        if (apiKey != null) {
            cfg.apiKey(apiKey);
        }
        if (kafkaBootstrap != null) {
            cfg.kafkaBootstrapServers(kafkaBootstrap).kafkaTopic(kafkaTopic);
        }

        DeliveryChannel channel = DeliveryChannel.valueOf(delivery.toUpperCase());
        BatchOptions options = BatchOptions.builder()
            .continueOnError(continueOnError)
            .progress(ProgressListener.loggingEvery(progressEvery))
            .build();

        try (LedgerClient client = LedgerClient.create(cfg.build())) {
            BatchResult<IngestionResult> batch = client.files().process(Path.of(file), channel, options);
            System.out.printf("Done. %s%n", batch);
            if (batch.hasFailures() && !continueOnError) {
                System.exit(1);
            }
            if (batch.hasFailures()) {
                System.err.printf("Failures: %d (see progress log)%n", batch.failureCount());
                System.exit(1);
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
              java -jar ledger-engine-sdk-*-cli.jar --file events.ndjson [options]

            Options:
              --base-url URL           Ledger base URL (default http://localhost:8080)
              --file PATH              JSON array or NDJSON of TransactionalEvent
              --delivery REST|KAFKA    Default REST
              --continue-on-error      Collect per-item failures (do not stop)
              --progress-every N       Log progress every N items (default 100)
              --token TOKEN            Bearer token (or LEDGER_TOKEN)
              --api-key KEY            API key header (or LEDGER_API_KEY)
              --external-type TYPE     Optional partner/system id
              --kafka-bootstrap HOSTS  Required for KAFKA delivery
              --kafka-topic TOPIC      Default ledger.transaction.events
            """);
    }
}
