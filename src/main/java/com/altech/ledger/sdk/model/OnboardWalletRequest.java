package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Agreement object to create one customer wallet (Phase 1).
 * Matches engine {@code OnboardWalletRequest}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class OnboardWalletRequest {
    private String userId;
    private String currency;
    private String name;
    private String externalId;
    private String externalType;

    public OnboardWalletRequest() {}

    public OnboardWalletRequest(String userId, String currency, String name,
                                String externalId, String externalType) {
        this.userId = userId;
        this.currency = currency;
        this.name = name;
        this.externalId = externalId;
        this.externalType = externalType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getExternalType() { return externalType; }
    public void setExternalType(String externalType) { this.externalType = externalType; }

    public void validate() {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (currency == null || !currency.matches("[A-Z]{2,4}")) {
            throw new IllegalArgumentException("currency must be 2-4 uppercase letters");
        }
    }

    public static final class Builder {
        private String userId;
        private String currency = "LP";
        private String name;
        private String externalId;
        private String externalType;

        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder externalId(String externalId) { this.externalId = externalId; return this; }
        public Builder externalType(String externalType) { this.externalType = externalType; return this; }

        public OnboardWalletRequest build() {
            OnboardWalletRequest r = new OnboardWalletRequest(userId, currency, name, externalId, externalType);
            r.validate();
            return r;
        }
    }
}
