package com.altech.ledger.sdk;

public class LedgerException extends RuntimeException {
    private final int httpStatus;
    private final String body;

    public LedgerException(String message) {
        super(message);
        this.httpStatus = -1;
        this.body = null;
    }

    public LedgerException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = -1;
        this.body = null;
    }

    public LedgerException(int httpStatus, String message, String body) {
        super(message + (body == null || body.isBlank() ? "" : " | body=" + body));
        this.httpStatus = httpStatus;
        this.body = body;
    }

    public int getHttpStatus() { return httpStatus; }
    public String getBody() { return body; }
}
