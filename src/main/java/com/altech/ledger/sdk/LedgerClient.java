package com.altech.ledger.sdk;

import com.altech.ledger.sdk.file.FileLedgerClient;
import com.altech.ledger.sdk.kafka.KafkaLedgerClient;
import com.altech.ledger.sdk.kafka.PublishResult;
import com.altech.ledger.sdk.model.*;
import com.altech.ledger.sdk.rest.RestLedgerClient;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Facade for product clients (first client: UAfinance).
 * <p>
 * Channels:
 * <ol>
 *   <li>{@link #rest()} — HTTP to ledger-engine</li>
 *   <li>{@link #kafka()} — MQ publish of {@link TransactionalEvent}</li>
 *   <li>{@link #file()} — batch file ingest (optional)</li>
 * </ol>
 */
public final class LedgerClient implements AutoCloseable {
    private final LedgerClientConfig config;
    private final RestLedgerClient rest;
    private KafkaLedgerClient kafka;
    private FileLedgerClient file;

    private LedgerClient(LedgerClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.rest = new RestLedgerClient(config);
    }

    public static LedgerClient create(LedgerClientConfig config) {
        return new LedgerClient(config);
    }

    public static LedgerClient create(String baseUrl) {
        return create(LedgerClientConfig.builder().baseUrl(baseUrl).build());
    }

    /**
     * UAfinance-oriented defaults: currency LP, externalType {@code uafinance}.
     */
    public static LedgerClient forUafinance(String baseUrl) {
        return create(LedgerClientConfig.builder()
            .baseUrl(baseUrl)
            .defaultCurrency("LP")
            .defaultExternalType("uafinance")
            .build());
    }

    public LedgerClientConfig config() {
        return config;
    }

    public RestLedgerClient rest() {
        return rest;
    }

    /**
     * Lazy Kafka channel. Requires {@link LedgerClientConfig.Builder#kafkaBootstrapServers(String)}
     * and {@code kafka-clients} on the classpath.
     */
    public synchronized KafkaLedgerClient kafka() {
        if (kafka == null) {
            kafka = new KafkaLedgerClient(config);
        }
        return kafka;
    }

    /**
     * File channel uses REST by default; Kafka delivery available when Kafka is configured.
     */
    public synchronized FileLedgerClient file() {
        if (file == null) {
            file = new FileLedgerClient(rest, config.getKafkaBootstrapServers() == null ? null : kafka());
        }
        return file;
    }

    // ---- convenience: Phase 1 wallets ----

    public WalletOnboardResponse onboardWallet(OnboardWalletRequest request) {
        return rest.onboardWallet(request);
    }

    public BatchOnboardWalletResponse onboardWalletsBatch(List<OnboardWalletRequest> wallets) {
        return rest.onboardWalletsBatch(wallets);
    }

    public WalletOnboardResponse getWallet(String ownerId, String currency) {
        return rest.getWallet(ownerId, currency);
    }

    // ---- convenience: Phase 2 transactions ----

    /** Channel 1 — REST ingest (sync result with points / LP reflection status). */
    public IngestionResult ingestRest(TransactionalEvent event) {
        return rest.ingestTransaction(event);
    }

    /** Channel 2 — Kafka publish (async processing on engine). */
    public PublishResult ingestKafka(TransactionalEvent event) {
        return kafka().publish(event);
    }

    /** Channel 3 — file batch via REST. */
    public List<IngestionResult> ingestFileRest(Path file) {
        return file().process(file, FileLedgerClient.Delivery.REST);
    }

    /** Channel 3 — file batch via Kafka. */
    public List<IngestionResult> ingestFileKafka(Path file) {
        return file().process(file, FileLedgerClient.Delivery.KAFKA);
    }

    @Override
    public void close() {
        rest.close();
        if (kafka != null) {
            kafka.close();
        }
    }
}
