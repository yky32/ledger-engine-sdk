package com.altech.ledger.sdk;

import com.altech.ledger.sdk.error.ApiError;

/**
 * Base exception for all SDK failures (client validation, HTTP, Kafka, network).
 * Prefer catching typed subclasses when recovering.
 */
public class LedgerException extends RuntimeException {
    private final int httpStatus;
    private final String code;
    private final String body;
    private final String requestId;
    private final String path;
    private final ApiError apiError;

    public LedgerException(String message) {
        this(message, null, -1, null, null, null, null);
    }

    public LedgerException(String message, Throwable cause) {
        this(message, cause, -1, null, null, null, null);
    }

    public LedgerException(int httpStatus, String message, String body) {
        this(message, null, httpStatus, null, body, null, null);
    }

    public LedgerException(String message, Throwable cause, int httpStatus, String code,
                           String body, String requestId, ApiError apiError) {
        super(buildMessage(message, httpStatus, code, requestId, body), cause);
        this.httpStatus = httpStatus;
        this.code = code;
        this.body = body;
        this.requestId = requestId;
        this.path = apiError == null ? null : apiError.getPath();
        this.apiError = apiError;
    }

    private static String buildMessage(String message, int httpStatus, String code,
                                       String requestId, String body) {
        StringBuilder sb = new StringBuilder(message == null ? "ledger SDK error" : message);
        if (httpStatus > 0) {
            sb.append(" [http=").append(httpStatus).append(']');
        }
        if (code != null && !code.isBlank()) {
            sb.append(" [code=").append(code).append(']');
        }
        if (requestId != null && !requestId.isBlank()) {
            sb.append(" [requestId=").append(requestId).append(']');
        }
        if (body != null && !body.isBlank() && (message == null || !message.contains(body))) {
            String snippet = body.length() > 500 ? body.substring(0, 500) + "…" : body;
            sb.append(" | body=").append(snippet);
        }
        return sb.toString();
    }

    public int getHttpStatus() { return httpStatus; }
    public String getCode() { return code; }
    public String getBody() { return body; }
    public String getRequestId() { return requestId; }
    public String getPath() { return path; }
    public ApiError getApiError() { return apiError; }

    public boolean isRetryable() {
        return false;
    }
}
