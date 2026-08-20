package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
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
    private Long movementId;
    private String matchedRuleCode;
    private Boolean dryRun;
    private List<?> eligibilityTrace;
    private List<?> legs;
    /** Correlation id from X-Request-Id (set by SDK). */
    private String requestId;

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
    public Long getMovementId() { return movementId; }
    public void setMovementId(Long movementId) { this.movementId = movementId; }
    public String getMatchedRuleCode() { return matchedRuleCode; }
    public void setMatchedRuleCode(String matchedRuleCode) { this.matchedRuleCode = matchedRuleCode; }
    public Boolean getDryRun() { return dryRun; }
    public void setDryRun(Boolean dryRun) { this.dryRun = dryRun; }
    public List<?> getEligibilityTrace() { return eligibilityTrace; }
    public void setEligibilityTrace(List<?> eligibilityTrace) { this.eligibilityTrace = eligibilityTrace; }
    public List<?> getLegs() { return legs; }
    public void setLegs(List<?> legs) { this.legs = legs; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public boolean isApplied() {
        return status == Status.EARNED || status == Status.BURNED || status == Status.PROCESSED;
    }

    public boolean isDryRun() {
        return Boolean.TRUE.equals(dryRun);
    }

    @Override
    public String toString() {
        return "IngestionResult{eventId='" + eventId + "', status=" + status
            + ", operation='" + operation + "', points=" + points
            + ", movementId=" + movementId + ", matchedRule=" + matchedRuleCode
            + ", dryRun=" + dryRun + ", requestId=" + requestId + "}";
    }
}
