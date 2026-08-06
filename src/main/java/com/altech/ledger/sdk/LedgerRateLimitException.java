package com.altech.ledger.sdk;

import com.altech.ledger.sdk.error.ApiError;

import java.time.Duration;

/** Rate limited (HTTP 429). May include Retry-After. */
public class LedgerRateLimitException extends LedgerException {
    private final Duration retryAfter;

    public LedgerRateLimitException(int httpStatus, String code, String message, String body,
                                    String requestId, ApiError apiError, Duration retryAfter) {
        super(message, null, httpStatus, code, body, requestId, apiError);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() { return retryAfter; }

    @Override
    public boolean isRetryable() {
        return true;
    }
}
