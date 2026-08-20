package com.altech.ledger.sdk.model;

import com.altech.ledger.sdk.LedgerValidationException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Wire contract for loyalty / transactional ingestion into ledger-engine.
 * Matches engine {@code TransactionalEvent} JSON.
 * <p>
 * Prefer {@link com.altech.ledger.sdk.api.UseCaseApi} so callers never build this manually.
 * Field {@code userId} is the CRM id; serialized as both {@code ownerId} and {@code userId}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
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

    /** CRM / wallet owner id. */
    @JsonProperty("userId")
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    /** Engine primary field — same value as {@link #getUserId()}. */
    @JsonProperty("ownerId")
    public String getOwnerId() { return userId; }

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
        require(userId, "userId/ownerId");
        require(eventType, "eventType");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new LedgerValidationException("amount must be >= 0");
        }
        require(currency, "currency");
        if (!currency.matches("[A-Z]{2,4}")) {
            throw new LedgerValidationException("currency must be 2-4 uppercase letters (e.g. LP)");
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    private static void require(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new LedgerValidationException(name + " is required");
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
        /** CRM id → engine ownerId. */
        public Builder userId(String userId) { this.userId = userId; return this; }
        /** Alias of {@link #userId(String)}. */
        public Builder ownerId(String ownerId) { this.userId = ownerId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }

        /** @deprecated use {@link #amount(BigDecimal)} */
        @Deprecated
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
