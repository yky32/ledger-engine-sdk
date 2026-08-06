package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response from ledger-engine after processing a {@link TransactionalEvent}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class IngestionResult {
    public enum Status { EARNED, BURNED, PROCESSED, SKIPPED, DUPLICATE, ERROR }

    private String eventId;
    private Status status;
    private String operation;
    private String reason;
    private BigDecimal points;
    private UUID transactionId;
    private String walletExternalReference;

    public IngestionResult() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public String getWalletExternalReference() { return walletExternalReference; }
    public void setWalletExternalReference(String walletExternalReference) {
        this.walletExternalReference = walletExternalReference;
    }

    public boolean isApplied() {
        return status == Status.EARNED || status == Status.BURNED || status == Status.PROCESSED;
    }

    @Override
    public String toString() {
        return "IngestionResult{eventId='" + eventId + "', status=" + status
            + ", operation='" + operation + "', points=" + points
            + ", reason='" + reason + "', wallet=" + walletExternalReference + "}";
    }
}
