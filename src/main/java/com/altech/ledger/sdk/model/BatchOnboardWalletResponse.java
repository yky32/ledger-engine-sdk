package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class BatchOnboardWalletResponse {
    private int requested;
    private int created;
    private int alreadyExists;
    private List<WalletOnboardResponse> createdWallets = new ArrayList<>();
    private List<String> alreadyExistingUserIds = new ArrayList<>();

    public int getRequested() { return requested; }
    public void setRequested(int requested) { this.requested = requested; }
    public int getCreated() { return created; }
    public void setCreated(int created) { this.created = created; }
    public int getAlreadyExists() { return alreadyExists; }
    public void setAlreadyExists(int alreadyExists) { this.alreadyExists = alreadyExists; }
    public List<WalletOnboardResponse> getCreatedWallets() { return createdWallets; }
    public void setCreatedWallets(List<WalletOnboardResponse> createdWallets) {
        this.createdWallets = createdWallets;
    }
    public List<String> getAlreadyExistingUserIds() { return alreadyExistingUserIds; }
    public void setAlreadyExistingUserIds(List<String> alreadyExistingUserIds) {
        this.alreadyExistingUserIds = alreadyExistingUserIds;
    }
}
