package com.altech.ledger.sdk.api;

import com.altech.ledger.sdk.model.UseCaseDescriptor;
import com.altech.ledger.sdk.rest.RestLedgerClient;

import java.util.List;
import java.util.Objects;

/**
 * Discover ops-configured use cases / COA bindings from LedgeRX.
 * <pre>
 * List&lt;UseCaseDescriptor&gt; all = client.catalog().listUseCases();
 * UseCaseDescriptor like = client.catalog().get("LIKE_FB_PAGE");
 * client.useCases().invoke(like.getCode(), ownerId, eventId, null, null, Map.of());
 * </pre>
 */
public final class CatalogApi {
    private final RestLedgerClient rest;

    public CatalogApi(RestLedgerClient rest) {
        this.rest = Objects.requireNonNull(rest, "rest");
    }

    /** Enabled Brain rules only (default). */
    public List<UseCaseDescriptor> listUseCases() {
        return rest.listUseCases(true);
    }

    public List<UseCaseDescriptor> listUseCases(boolean enabledOnly) {
        return rest.listUseCases(enabledOnly);
    }

    public UseCaseDescriptor get(String code) {
        return rest.getUseCase(code);
    }
}
