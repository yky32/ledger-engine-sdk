package com.altech.ledger.sdk;

import com.altech.ledger.sdk.error.ApiError;

/** Authentication / authorization failure (HTTP 401 / 403). */
public class LedgerAuthException extends LedgerException {
    public LedgerAuthException(int httpStatus, String code, String message, String body,
                               String requestId, ApiError apiError) {
        super(message, null, httpStatus, code, body, requestId, apiError);
    }
}
