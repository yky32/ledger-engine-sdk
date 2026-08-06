package com.altech.ledger.sdk.batch;

import java.util.Objects;

/**
 * Options for batch onboard / event ingest / file process.
 */
public final class BatchOptions {
    private final boolean continueOnError;
    private final ProgressListener progress;
    private final int engineBatchSize;

    private BatchOptions(Builder b) {
        this.continueOnError = b.continueOnError;
        this.progress = b.progress == null ? ProgressListener.noop() : b.progress;
        this.engineBatchSize = b.engineBatchSize <= 0 ? 1000 : Math.min(b.engineBatchSize, 1000);
    }

    public static BatchOptions defaults() {
        return builder().build();
    }

    /** Fail-fast on first item error (default). */
    public static BatchOptions failFast() {
        return builder().continueOnError(false).build();
    }

    /** Collect per-item failures; do not stop the batch. */
    public static BatchOptions continueOnError() {
        return builder().continueOnError(true).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isContinueOnError() { return continueOnError; }
    public ProgressListener getProgress() { return progress; }
    /** Max wallets per engine {@code /wallets/batch} call (1–1000). */
    public int getEngineBatchSize() { return engineBatchSize; }

    public BatchOptions withProgress(ProgressListener progress) {
        return builder()
            .continueOnError(continueOnError)
            .progress(progress)
            .engineBatchSize(engineBatchSize)
            .build();
    }

    public static final class Builder {
        private boolean continueOnError = false;
        private ProgressListener progress;
        private int engineBatchSize = 1000;

        public Builder continueOnError(boolean continueOnError) {
            this.continueOnError = continueOnError; return this;
        }

        public Builder progress(ProgressListener progress) {
            this.progress = progress; return this;
        }

        public Builder engineBatchSize(int engineBatchSize) {
            this.engineBatchSize = engineBatchSize; return this;
        }

        public BatchOptions build() {
            return new BatchOptions(this);
        }
    }

    @Override
    public String toString() {
        return "BatchOptions{continueOnError=" + continueOnError
            + ", engineBatchSize=" + engineBatchSize + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BatchOptions that)) return false;
        return continueOnError == that.continueOnError && engineBatchSize == that.engineBatchSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(continueOnError, engineBatchSize);
    }
}
