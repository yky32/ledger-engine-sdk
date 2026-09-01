package com.altech.ledger.sdk.model;

import com.altech.ledger.sdk.LedgerValidationException;
import com.fasterxml.jackson.annotation.JsonAlias;
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
 * Identity: {@code ownerId} (e.g. 01A…), optional {@code mainAccount} (e.g. 9089… / 9088…),
 * and {@code metadata} (client hashmap for Door/Brain/COA).
 * {@code userId} is accepted as an alias of {@code ownerId}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TransactionalEvent {
    private String eventId;
    @JsonProperty("ownerId")
    @JsonAlias({"userId", "associatedIdentifier"})
    private String ownerId;
    private String eventType;
    private BigDecimal amount;
    private String currency;
    private Instant occurredAt;
    private Map<String, String> metadata;
    @JsonProperty("mainAccount")
    @JsonAlias({"main_account"})
    private String mainAccount;

    public TransactionalEvent() {}

    public TransactionalEvent(String eventId, String ownerId, String eventType,
                              BigDecimal amount, String currency,
                              Instant occurredAt, Map<String, String> metadata) {
        this(eventId, ownerId, eventType, amount, currency, occurredAt, metadata, null);
    }

    public TransactionalEvent(String eventId, String ownerId, String eventType,
                              BigDecimal amount, String currency,
                              Instant occurredAt, Map<String, String> metadata,
                              String mainAccount) {
        this.eventId = eventId;
        this.ownerId = ownerId;
        this.eventType = eventType;
        this.amount = amount;
        this.currency = currency;
        this.occurredAt = occurredAt;
        this.metadata = metadata;
        this.mainAccount = mainAccount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    /** CRM / wallet owner id — engine primary field. */
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    /** Alias of {@link #getOwnerId()}. */
    @JsonProperty("userId")
    public String getUserId() { return ownerId; }
    public void setUserId(String userId) { this.ownerId = userId; }

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
    public String getMainAccount() { return mainAccount; }
    public void setMainAccount(String mainAccount) { this.mainAccount = mainAccount; }

    public void validate() {
        require(eventId, "eventId");
        require(ownerId, "ownerId");
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
        private String ownerId;
        private String eventType;
        private BigDecimal amount;
        private String currency = "LP";
        private Instant occurredAt;
        private Map<String, String> metadata;
        private String mainAccount;

        public Builder eventId(String eventId) { this.eventId = eventId; return this; }
        public Builder ownerId(String ownerId) { this.ownerId = ownerId; return this; }
        /** Alias of {@link #ownerId(String)}. */
        public Builder userId(String userId) { this.ownerId = userId; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }

        /** @deprecated use {@link #amount(BigDecimal)} */
        @Deprecated
        public Builder amount(double amount) { this.amount = BigDecimal.valueOf(amount); return this; }

        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder occurredAt(Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder metadata(Map<String, String> metadata) { this.metadata = metadata; return this; }
        public Builder mainAccount(String mainAccount) { this.mainAccount = mainAccount; return this; }

        public TransactionalEvent build() {
            TransactionalEvent e = new TransactionalEvent(
                eventId, ownerId, eventType, amount, currency, occurredAt, metadata, mainAccount);
            e.validate();
            return e;
        }
    }
}
