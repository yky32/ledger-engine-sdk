package com.altech.ledger.sdk.batch;

/**
 * Callback for long-running batch / file ingest (e.g. UAfinance 70K).
 * Implementations must be fast and non-blocking.
 */
@FunctionalInterface
public interface ProgressListener {
    /**
     * @param index    zero-based index of the item just processed
     * @param total    total items if known, otherwise {@code -1} (streaming unknown length)
     * @param result   outcome of this item
     */
    void onItem(int index, int total, ItemResult<?> result);

    default void onComplete(BatchResult<?> batch) {
        // no-op
    }

    static ProgressListener noop() {
        return (index, total, result) -> {};
    }

    /** Simple stderr progress every {@code everyN} items. */
    static ProgressListener loggingEvery(int everyN) {
        int n = Math.max(1, everyN);
        return (index, total, result) -> {
            if ((index + 1) % n == 0 || result.isFailure()) {
                String tot = total >= 0 ? "/" + total : "";
                System.err.printf("ledger-sdk progress %d%s %s id=%s%n",
                    index + 1, tot, result.getOutcome(), result.getId());
            }
        };
    }
}
