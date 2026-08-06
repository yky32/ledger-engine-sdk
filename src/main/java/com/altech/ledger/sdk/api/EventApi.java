package com.altech.ledger.sdk.api;

import com.altech.ledger.sdk.DeliveryChannel;
import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.batch.BatchOptions;
import com.altech.ledger.sdk.batch.BatchResult;
import com.altech.ledger.sdk.batch.ItemResult;
import com.altech.ledger.sdk.kafka.KafkaLedgerClient;
import com.altech.ledger.sdk.kafka.PublishResult;
import com.altech.ledger.sdk.model.IngestionResult;
import com.altech.ledger.sdk.model.TransactionalEvent;
import com.altech.ledger.sdk.rest.RestLedgerClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Resource API — Phase 2 transactional events (POS / order → LP via engine rules).
 * <pre>
 * client.events().submit(event);                    // REST
 * client.events().submit(event, DeliveryChannel.KAFKA);
 * client.events().submitBatch(list, REST, BatchOptions.continueOnError());
 * </pre>
 */
public final class EventApi {
    private final RestLedgerClient rest;
    private final Supplier<KafkaLedgerClient> kafka;
    private final Executor asyncExecutor;

    public EventApi(RestLedgerClient rest, Supplier<KafkaLedgerClient> kafka, Executor asyncExecutor) {
        this.rest = Objects.requireNonNull(rest, "rest");
        this.kafka = Objects.requireNonNull(kafka, "kafka");
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor");
    }

    /** REST ingest (default channel). */
    public IngestionResult submit(TransactionalEvent event) {
        return rest.ingestTransaction(event);
    }

    /**
     * Submit on the given channel. Prefer {@link #submitRest} / {@link #submitKafka}
     * when you need a typed return value.
     */
    public void submit(TransactionalEvent event, DeliveryChannel channel) {
        Objects.requireNonNull(channel, "channel");
        switch (channel) {
            case REST -> rest.ingestTransaction(event);
            case KAFKA -> kafka.get().publish(event);
        }
    }

    public IngestionResult submitRest(TransactionalEvent event) {
        return rest.ingestTransaction(event);
    }

    public PublishResult submitKafka(TransactionalEvent event) {
        return kafka.get().publish(event);
    }

    public CompletableFuture<IngestionResult> submitAsync(TransactionalEvent event) {
        return CompletableFuture.supplyAsync(() -> submitRest(event), asyncExecutor);
    }

    /**
     * Batch submit with per-item results.
     * REST → {@link IngestionResult}; Kafka successes carry null value (use {@link PublishResult} via single submit).
     * For Kafka, {@link ItemResult#getValue()} is null on success (offset tracked only on single publish).
     */
    public BatchResult<IngestionResult> submitBatch(List<TransactionalEvent> events,
                                                    DeliveryChannel channel,
                                                    BatchOptions options) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(channel, "channel");
        BatchOptions opts = options == null ? BatchOptions.defaults() : options;
        if (events.isEmpty()) {
            return BatchResult.empty();
        }

        List<ItemResult<IngestionResult>> items = new ArrayList<>(events.size());
        for (int i = 0; i < events.size(); i++) {
            TransactionalEvent event = events.get(i);
            String id = event.getEventId();
            ItemResult<IngestionResult> ir;
            try {
                if (channel == DeliveryChannel.REST) {
                    IngestionResult result = rest.ingestTransaction(event);
                    if (result != null && (result.getStatus() == IngestionResult.Status.SKIPPED
                        || result.getStatus() == IngestionResult.Status.DUPLICATE)) {
                        ir = ItemResult.skipped(i, id, result);
                    } else {
                        ir = ItemResult.success(i, id, result);
                    }
                } else {
                    kafka.get().publish(event);
                    ir = ItemResult.success(i, id, null);
                }
            } catch (LedgerException ex) {
                ir = ItemResult.failure(i, id, ex);
                items.add(ir);
                opts.getProgress().onItem(i, events.size(), ir);
                if (!opts.isContinueOnError()) {
                    BatchResult<IngestionResult> partial = new BatchResult<>(items);
                    opts.getProgress().onComplete(partial);
                    throw ex;
                }
                continue;
            }
            items.add(ir);
            opts.getProgress().onItem(i, events.size(), ir);
        }
        if (channel == DeliveryChannel.KAFKA) {
            kafka.get().flush();
        }
        BatchResult<IngestionResult> batch = new BatchResult<>(items);
        opts.getProgress().onComplete(batch);
        return batch;
    }

    public BatchResult<IngestionResult> submitBatch(List<TransactionalEvent> events, BatchOptions options) {
        return submitBatch(events, DeliveryChannel.REST, options);
    }
}
