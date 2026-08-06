package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class AccountSnapshot {
    private Long id;
    private String fullNumber;
    private String currency;
    private BigDecimal ledgerBalance;
    private BigDecimal availableBalance;
    private String status;
    private Instant createDt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullNumber() { return fullNumber; }
    public void setFullNumber(String fullNumber) { this.fullNumber = fullNumber; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getLedgerBalance() { return ledgerBalance; }
    public void setLedgerBalance(BigDecimal ledgerBalance) { this.ledgerBalance = ledgerBalance; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreateDt() { return createDt; }
    public void setCreateDt(Instant createDt) { this.createDt = createDt; }
}
