package com.altech.ledger.sdk;

import com.altech.ledger.sdk.error.ApiError;

/** Conflict with existing state (HTTP 409), e.g. duplicate event or data integrity. */
public class LedgerConflictException extends LedgerException {
    public LedgerConflictException(int httpStatus, String code, String message, String body,
                                   String requestId, ApiError apiError) {
        super(message, null, httpStatus, code, body, requestId, apiError);
    }
}
