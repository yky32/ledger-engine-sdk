package com.altech.ledger.sdk.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Matches ledger-engine {@code GlobalExceptionHandler.ApiError} JSON shape.
 * <pre>
 * {
 *   "timestamp": "...",
 *   "status": 400,
 *   "code": "VALIDATION_FAILED",
 *   "message": "...",
 *   "path": "/wallets",
 *   "fieldErrors": { "userId": "must not be blank" }
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ApiError {
    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private String path;
    private Map<String, String> fieldErrors;

    public ApiError() {}

    public ApiError(Instant timestamp, int status, String code, String message,
                    String path, Map<String, String> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Map<String, String> getFieldErrors() {
        return fieldErrors == null ? Collections.emptyMap() : fieldErrors;
    }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }

    public boolean hasFieldErrors() {
        return fieldErrors != null && !fieldErrors.isEmpty();
    }

    @Override
    public String toString() {
        return "ApiError{status=" + status + ", code='" + code + "', message='" + message
            + "', path='" + path + "', fieldErrors=" + getFieldErrors() + "}";
    }
}
