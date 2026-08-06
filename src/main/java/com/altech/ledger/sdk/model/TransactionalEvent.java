package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Agreement object for loyalty / transactional ingestion into ledger-engine.
 * Matches engine {@code TransactionalEvent} JSON contract.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TransactionalEvent {
    private String eventId;
    private String userId;
    private String eventType;
    private BigDecimal amount;
    private String currency;
    private Instant occurredAt;
    private Map<String, String> metadata;

    public TransactionalEvent() {}

    public TransactionalEvent(String eventId, String userId, String eventType,
                              BigDecimal amount, String currency,
                              Instant occurredAt, Map<String, String> metadata) {
        this.eventId = eventId;
        this.userId = userId;
        this.eventType = eventType;
        this.amount = amount;
        this.currency = currency;
        this.occurredAt = occurredAt;
        this.metadata = metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }

    public void validate() {
        require(eventId, "eventId");
        require(userId, "userId");
        require(eventType, "eventType");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        require(currency, "currency");
        if (!currency.matches("[A-Z]{2,4}")) {
            throw new IllegalArgumentException("currency must be 2-4 uppercase letters (e.g. LP)");
        }
    }

    private static void require(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }

    public static final class Builder {
        private String eventId;
        private String userId;
        private String eventType;
        private BigDecimal amount;
        private String currency = "LP";
        private Instant occurredAt;
        private Map<String, String> metadata;

        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder amount(double amount) { this.amount = BigDecimal.valueOf(amount); return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }

        public TransactionalEvent build() {
            TransactionalEvent e = new TransactionalEvent(
                eventId, userId, eventType, amount, currency, occurredAt, metadata);
            e.validate();
            return e;
        }
    }
}
