package com.altech.ledger.sdk;

/** Transport failure: timeout, DNS, connection reset, etc. */
public class LedgerNetworkException extends LedgerException {
    public LedgerNetworkException(String message, Throwable cause) {
        super(message, cause, -1, "NETWORK_ERROR", null, null, null);
    }

    @Override
    public boolean isRetryable() {
        return true;
    }
}
