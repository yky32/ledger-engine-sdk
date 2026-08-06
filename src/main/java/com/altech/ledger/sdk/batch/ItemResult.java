package com.altech.ledger.sdk.batch;

import com.altech.ledger.sdk.LedgerException;

/**
 * Result for a single item in a batch operation.
 *
 * @param <T> success payload type (e.g. {@code IngestionResult}, {@code WalletOnboardResponse})
 */
public final class ItemResult<T> {
    private final int index;
    private final String id;
    private final ItemOutcome outcome;
    private final T value;
    private final LedgerException error;

    private ItemResult(int index, String id, ItemOutcome outcome, T value, LedgerException error) {
        this.index = index;
        this.id = id;
        this.outcome = outcome;
        this.value = value;
        this.error = error;
    }

    public static <T> ItemResult<T> success(int index, String id, T value) {
        return new ItemResult<>(index, id, ItemOutcome.SUCCESS, value, null);
    }

    public static <T> ItemResult<T> skipped(int index, String id, T value) {
        return new ItemResult<>(index, id, ItemOutcome.SKIPPED, value, null);
    }

    public static <T> ItemResult<T> failure(int index, String id, LedgerException error) {
        return new ItemResult<>(index, id, ItemOutcome.FAILURE, null, error);
    }

    public int getIndex() { return index; }
    public String getId() { return id; }
    public ItemOutcome getOutcome() { return outcome; }
    public T getValue() { return value; }
    public LedgerException getError() { return error; }

    public boolean isSuccess() { return outcome == ItemOutcome.SUCCESS; }
    public boolean isFailure() { return outcome == ItemOutcome.FAILURE; }
    public boolean isSkipped() { return outcome == ItemOutcome.SKIPPED; }

    @Override
    public String toString() {
        return "ItemResult{index=" + index + ", id='" + id + "', outcome=" + outcome
            + (error != null ? ", error=" + error.getMessage() : "")
            + (value != null ? ", value=" + value : "")
            + "}";
    }
}
