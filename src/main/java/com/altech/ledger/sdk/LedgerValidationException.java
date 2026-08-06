package com.altech.ledger.sdk;

import com.altech.ledger.sdk.error.ApiError;

import java.util.Collections;
import java.util.Map;

/** Client-side validation failure or HTTP 400 from the engine. */
public class LedgerValidationException extends LedgerException {
    private final Map<String, String> fieldErrors;

    public LedgerValidationException(String message) {
        this(message, null, -1, "VALIDATION_FAILED", null, null, null, Map.of());
    }

    public LedgerValidationException(String message, Throwable cause) {
        this(message, cause, -1, "VALIDATION_FAILED", null, null, null, Map.of());
    }

    public LedgerValidationException(int httpStatus, String code, String message, String body,
                                     String requestId, ApiError apiError) {
        this(message, null, httpStatus, code, body, requestId, apiError,
            apiError == null ? Map.of() : apiError.getFieldErrors());
    }

    private LedgerValidationException(String message, Throwable cause, int httpStatus, String code,
                                      String body, String requestId, ApiError apiError,
                                      Map<String, String> fieldErrors) {
        super(message, cause, httpStatus, code, body, requestId, apiError);
        this.fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }
}
