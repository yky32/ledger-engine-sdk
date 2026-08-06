package com.altech.ledger.sdk;

import com.altech.ledger.sdk.api.EventApi;
import com.altech.ledger.sdk.api.FileApi;
import com.altech.ledger.sdk.api.WalletApi;
import com.altech.ledger.sdk.batch.BatchOptions;
import com.altech.ledger.sdk.batch.BatchResult;
import com.altech.ledger.sdk.file.FileLedgerClient;
import com.altech.ledger.sdk.kafka.KafkaLedgerClient;
import com.altech.ledger.sdk.kafka.PublishResult;
import com.altech.ledger.sdk.model.*;
import com.altech.ledger.sdk.rest.RestLedgerClient;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Facade for product clients (first client: UAfinance).
 * <p>
 * Preferred resource API (Phase B):
 * <pre>
 * client.wallets().onboard(...);
 * client.events().submit(event);
 * client.files().process(path, DeliveryChannel.REST, BatchOptions.continueOnError());
 * </pre>
 * Channels still available via {@link #rest()}, {@link #kafka()}, {@link #file()}.
 */
public final class LedgerClient implements AutoCloseable {
    private final LedgerClientConfig config;
    private final RestLedgerClient rest;
    private final Executor asyncExecutor;
    private final WalletApi wallets;
    private final EventApi events;
    private final FileApi files;
    private KafkaLedgerClient kafka;
    private FileLedgerClient file;

    private LedgerClient(LedgerClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.rest = new RestLedgerClient(config);
        this.asyncExecutor = ForkJoinPool.commonPool();
        this.wallets = new WalletApi(rest, asyncExecutor);
        this.events = new EventApi(rest, this::kafka, asyncExecutor);
        this.files = new FileApi(rest, this::kafka);
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

    // ---- Phase B resource API ----

    /** Phase 1 — wallet onboarding & lookup. */
    public WalletApi wallets() {
        return wallets;
    }

    /** Phase 2 — transactional events (REST / Kafka). */
    public EventApi events() {
        return events;
    }

    /** Phase 3 — streaming file ingest. */
    public FileApi files() {
        return files;
    }

    // ---- low-level channels ----

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
     * Legacy file channel (list-based). Prefer {@link #files()} for streaming + batch options.
     */
    public synchronized FileLedgerClient file() {
        if (file == null) {
            file = new FileLedgerClient(rest, config.getKafkaBootstrapServers() == null ? null : kafka());
        }
        return file;
    }

    // ---- convenience (1.0-compatible) ----

    public WalletOnboardResponse onboardWallet(OnboardWalletRequest request) {
        return wallets.onboard(request);
    }

    public BatchOnboardWalletResponse onboardWalletsBatch(List<OnboardWalletRequest> wallets) {
        return this.wallets.onboardBatch(wallets);
    }

    public WalletOnboardResponse getWallet(String ownerId, String currency) {
        return wallets.get(ownerId, currency);
    }

    /** Channel 1 — REST ingest. */
    public IngestionResult ingestRest(TransactionalEvent event) {
        return events.submitRest(event);
    }

    /** Channel 2 — Kafka publish. */
    public PublishResult ingestKafka(TransactionalEvent event) {
        return events.submitKafka(event);
    }

    /**
     * Channel 3 — file via REST (fail-fast). Prefer
     * {@code files().process(path, REST, BatchOptions.continueOnError())} for large files.
     */
    public List<IngestionResult> ingestFileRest(Path path) {
        BatchResult<IngestionResult> batch = files.process(path, DeliveryChannel.REST, BatchOptions.failFast());
        batch.throwIfAnyFailed();
        return batch.successes();
    }

    /** Channel 3 — file via Kafka. */
    public List<IngestionResult> ingestFileKafka(Path path) {
        BatchResult<IngestionResult> batch = files.process(path, DeliveryChannel.KAFKA, BatchOptions.failFast());
        batch.throwIfAnyFailed();
        return batch.successes();
    }

    @Override
    public void close() {
        rest.close();
        if (kafka != null) {
            kafka.close();
        }
    }
}
