package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/** Max 1000 wallets per call (engine constraint). */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BatchOnboardWalletRequest {
    private List<OnboardWalletRequest> wallets = new ArrayList<>();

    public BatchOnboardWalletRequest() {}

    public BatchOnboardWalletRequest(List<OnboardWalletRequest> wallets) {
        this.wallets = wallets;
    }

    public List<OnboardWalletRequest> getWallets() { return wallets; }
    public void setWallets(List<OnboardWalletRequest> wallets) { this.wallets = wallets; }
}
