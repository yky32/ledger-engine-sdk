package com.altech.ledger.sdk;

import com.altech.ledger.sdk.error.ApiError;

/** Resource not found (HTTP 404). */
public class LedgerNotFoundException extends LedgerException {
    public LedgerNotFoundException(int httpStatus, String code, String message, String body,
                                   String requestId, ApiError apiError) {
        super(message, null, httpStatus, code, body, requestId, apiError);
    }
}
