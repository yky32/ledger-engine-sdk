package com.altech.ledger.sdk;

import com.altech.ledger.sdk.error.ApiError;

/** Engine / gateway server error (HTTP 5xx). */
public class LedgerServerException extends LedgerException {
    public LedgerServerException(int httpStatus, String code, String message, String body,
                                 String requestId, ApiError apiError) {
        super(message, null, httpStatus, code, body, requestId, apiError);
    }

    @Override
    public boolean isRetryable() {
        return getHttpStatus() == 502 || getHttpStatus() == 503 || getHttpStatus() == 504
            || getHttpStatus() == 500;
    }
}
