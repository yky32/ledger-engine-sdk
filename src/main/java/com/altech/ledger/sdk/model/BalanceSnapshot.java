package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class BalanceSnapshot {
    private Long accountId;
    private String currency;
    private BigDecimal debitTotal;
    private BigDecimal creditTotal;
    private BigDecimal balance;
    private BigDecimal ledgerBalance;
    private BigDecimal availableBalance;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getDebitTotal() { return debitTotal; }
    public void setDebitTotal(BigDecimal debitTotal) { this.debitTotal = debitTotal; }
    public BigDecimal getCreditTotal() { return creditTotal; }
    public void setCreditTotal(BigDecimal creditTotal) { this.creditTotal = creditTotal; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getLedgerBalance() { return ledgerBalance; }
    public void setLedgerBalance(BigDecimal ledgerBalance) { this.ledgerBalance = ledgerBalance; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
}
