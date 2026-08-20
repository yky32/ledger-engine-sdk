package com.altech.ledger.sdk.api;

import com.altech.ledger.sdk.LedgerValidationException;
import com.altech.ledger.sdk.model.UseCaseDescriptor;
import com.altech.ledger.sdk.rest.RestLedgerClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Discover ops-configured use cases / COA bindings from LedgeRX.
 * Supports optional TTL cache for xapi (avoid hammering Admin config on every request).
 */
public final class CatalogApi {
    private final RestLedgerClient rest;
    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();
    private volatile Duration defaultTtl = Duration.ofMinutes(5);

    public CatalogApi(RestLedgerClient rest) {
        this.rest = Objects.requireNonNull(rest, "rest");
    }

    /** Default cache TTL for {@link #listUseCasesCached()}. */
    public CatalogApi cacheTtl(Duration ttl) {
        this.defaultTtl = ttl == null ? Duration.ZERO : ttl;
        return this;
    }

    /** Enabled Brain rules only (live, no cache). */
    public List<UseCaseDescriptor> listUseCases() {
        return rest.listUseCases(true);
    }

    public List<UseCaseDescriptor> listUseCases(boolean enabledOnly) {
        return rest.listUseCases(enabledOnly);
    }

    /**
     * Cached list (enabled only). Thread-safe; refresh when TTL expires.
     */
    public List<UseCaseDescriptor> listUseCasesCached() {
        return listUseCasesCached(defaultTtl);
    }

    public List<UseCaseDescriptor> listUseCasesCached(Duration ttl) {
        Duration t = ttl == null ? Duration.ZERO : ttl;
        CacheEntry cur = cache.get();
        Instant now = Instant.now();
        if (cur != null && t.toMillis() > 0 && cur.expiresAt.isAfter(now)) {
            return cur.items;
        }
        List<UseCaseDescriptor> fresh = rest.listUseCases(true);
        cache.set(new CacheEntry(fresh, now.plus(t.toMillis() <= 0 ? Duration.ofMillis(1) : t)));
        return fresh;
    }

    public void invalidateCache() {
        cache.set(null);
    }

    public UseCaseDescriptor get(String code) {
        return rest.getUseCase(code);
    }

    /**
     * Find in cached catalog by code; throws if missing.
     */
    public UseCaseDescriptor require(String code) {
        String n = norm(code);
        return listUseCasesCached().stream()
            .filter(u -> n.equals(norm(u.getCode())))
            .findFirst()
            .orElseThrow(() -> new LedgerValidationException(
                "use case not in catalog (enabled): " + code + " — configure Brain in LedgeRX Admin"));
    }

    private static String norm(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private record CacheEntry(List<UseCaseDescriptor> items, Instant expiresAt) {}
}
