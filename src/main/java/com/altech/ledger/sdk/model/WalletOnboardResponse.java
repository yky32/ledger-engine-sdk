package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class WalletOnboardResponse {
    private Long walletId;
    private String alias;
    private String ownerId;
    private String currency;
    private String status;
    private String externalId;
    private String externalType;
    private AccountSnapshot account;
    private BalanceSnapshot balance;

    public Long getWalletId() { return walletId; }
    public void setWalletId(Long walletId) { this.walletId = walletId; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getExternalType() { return externalType; }
    public void setExternalType(String externalType) { this.externalType = externalType; }
    public AccountSnapshot getAccount() { return account; }
    public void setAccount(AccountSnapshot account) { this.account = account; }
    public BalanceSnapshot getBalance() { return balance; }
    public void setBalance(BalanceSnapshot balance) { this.balance = balance; }
}
