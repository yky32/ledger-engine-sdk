package com.altech.ledger.sdk.error;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Exponential backoff with full jitter for retryable REST failures.
 */
public final class RetryPolicy {
    private final int maxRetries;
    private final Duration initialDelay;
    private final Duration maxDelay;

    public RetryPolicy(int maxRetries, Duration initialDelay, Duration maxDelay) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        this.maxRetries = maxRetries;
        this.initialDelay = Objects.requireNonNullElse(initialDelay, Duration.ofMillis(200));
        this.maxDelay = Objects.requireNonNullElse(maxDelay, Duration.ofSeconds(5));
    }

    public static RetryPolicy defaults() {
        return new RetryPolicy(3, Duration.ofMillis(200), Duration.ofSeconds(5));
    }

    public static RetryPolicy none() {
        return new RetryPolicy(0, Duration.ofMillis(0), Duration.ofMillis(0));
    }

    public int getMaxRetries() { return maxRetries; }
    public Duration getInitialDelay() { return initialDelay; }
    public Duration getMaxDelay() { return maxDelay; }

    public boolean shouldRetry(int attempt, int httpStatus) {
        if (attempt >= maxRetries) {
            return false;
        }
        return httpStatus == 429 || httpStatus == 500 || httpStatus == 502
            || httpStatus == 503 || httpStatus == 504;
    }

    public boolean shouldRetryNetwork(int attempt) {
        return attempt < maxRetries;
    }

    /**
     * Full jitter: random in [0, min(maxDelay, initial * 2^attempt)].
     */
    public Duration delayForAttempt(int attempt) {
        if (initialDelay.isZero() || maxDelay.isZero()) {
            return Duration.ZERO;
        }
        long baseMs = initialDelay.toMillis();
        long exp = baseMs * (1L << Math.min(attempt, 10));
        long capped = Math.min(exp, maxDelay.toMillis());
        if (capped <= 0) {
            return Duration.ZERO;
        }
        long jitter = ThreadLocalRandom.current().nextLong(capped + 1);
        return Duration.ofMillis(jitter);
    }

    public void sleep(int attempt) {
        Duration d = delayForAttempt(attempt);
        if (d.isZero() || d.isNegative()) {
            return;
        }
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new com.altech.ledger.sdk.LedgerNetworkException("Retry sleep interrupted", ie);
        }
    }
}
