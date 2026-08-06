package com.altech.ledger.sdk.batch;

import com.altech.ledger.sdk.LedgerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregate result of a batch operation with per-item outcomes.
 */
public final class BatchResult<T> {
    private final List<ItemResult<T>> items;

    public BatchResult(List<ItemResult<T>> items) {
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
    }

    public List<ItemResult<T>> getItems() {
        return items;
    }

    public int size() {
        return items.size();
    }

    public int successCount() {
        return (int) items.stream().filter(ItemResult::isSuccess).count();
    }

    public int failureCount() {
        return (int) items.stream().filter(ItemResult::isFailure).count();
    }

    public int skippedCount() {
        return (int) items.stream().filter(ItemResult::isSkipped).count();
    }

    public boolean allSucceeded() {
        return failureCount() == 0;
    }

    public boolean hasFailures() {
        return failureCount() > 0;
    }

    public List<T> successes() {
        return items.stream()
            .filter(ItemResult::isSuccess)
            .map(ItemResult::getValue)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<ItemResult<T>> failures() {
        return items.stream().filter(ItemResult::isFailure).toList();
    }

    /**
     * Throws the first failure if any item failed (useful after {@code continueOnError}).
     */
    public void throwIfAnyFailed() {
        for (ItemResult<T> item : items) {
            if (item.isFailure() && item.getError() != null) {
                throw item.getError();
            }
        }
    }

    /**
     * Throws a summary {@link LedgerException} listing failed ids when any failed.
     */
    public void throwSummaryIfAnyFailed() {
        if (!hasFailures()) {
            return;
        }
        String ids = failures().stream()
            .map(ItemResult::getId)
            .filter(Objects::nonNull)
            .limit(20)
            .collect(Collectors.joining(", "));
        throw new LedgerException(
            "Batch completed with " + failureCount() + " failure(s) of " + size()
                + " (e.g. " + ids + (failureCount() > 20 ? ", …" : "") + ")");
    }

    public static <T> BatchResult<T> empty() {
        return new BatchResult<>(Collections.emptyList());
    }

    @Override
    public String toString() {
        return "BatchResult{size=" + size()
            + ", success=" + successCount()
            + ", failed=" + failureCount()
            + ", skipped=" + skippedCount() + "}";
    }
}
