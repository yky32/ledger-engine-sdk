package com.altech.ledger.sdk.api;

import com.altech.ledger.sdk.model.EventTypes;
import com.altech.ledger.sdk.model.IngestionResult;
import com.altech.ledger.sdk.model.TransactionalEvent;
import com.altech.ledger.sdk.LedgerValidationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * High-level product use-cases. Upstream calls these — does <b>not</b> hand-build engine JSON.
 * <p>
 * SDK fills the engine contract ({@code eventId}, {@code ownerId}/{@code userId}, {@code eventType},
 * {@code amount}, {@code currency}, {@code occurredAt}, {@code metadata}).
 * <pre>
 * client.useCases().likeFacebookPage("CUST-1", "like-001", "ua-finance-page");
 * client.useCases().ccTxnLp("CUST-1", "txn-9", new BigDecimal("500"), "HKD", "5411");
 * client.useCases().purchase("CUST-1", "ord-1", new BigDecimal("150"), "HKD");
 * </pre>
 */
public final class UseCaseApi {
    private final EventApi events;

    public UseCaseApi(EventApi events) {
        this.events = Objects.requireNonNull(events, "events");
    }

    /**
     * Non-financial: customer liked a Facebook page → engine Brain FIXED (e.g. +5 LP).
     *
     * @param ownerId CRM / wallet owner id
     * @param eventId idempotency key (unique per like action)
     * @param pageId  optional page identifier stored in metadata
     */
    public IngestionResult likeFacebookPage(String ownerId, String eventId, String pageId) {
        Map<String, String> md = new HashMap<>();
        md.put("channel", "facebook");
        if (pageId != null && !pageId.isBlank()) {
            md.put("pageId", pageId.trim());
        }
        md.put("useCase", EventTypes.LIKE_FB_PAGE);
        return events.submit(engagement(EventTypes.LIKE_FB_PAGE, ownerId, eventId, md));
    }

    /** Same as {@link #likeFacebookPage} with generated eventId. */
    public IngestionResult likeFacebookPage(String ownerId, String pageId) {
        return likeFacebookPage(ownerId, "like-fb-" + UUID.randomUUID(), pageId);
    }

    public IngestionResult followInstagram(String ownerId, String eventId, String handle) {
        Map<String, String> md = new HashMap<>();
        md.put("channel", "instagram");
        if (handle != null) {
            md.put("handle", handle);
        }
        md.put("useCase", EventTypes.FOLLOW_IG);
        return events.submit(engagement(EventTypes.FOLLOW_IG, ownerId, eventId, md));
    }

    /**
     * Card spend → LP (engine recipe {@code CC_TXN_LP} + Brain formula).
     *
     * @param spendAmount original spend (e.g. 500 HKD); points from Brain RATE/FIXED
     * @param spendCurrency e.g. HKD
     */
    public IngestionResult ccTxnLp(String ownerId, String eventId,
                                   BigDecimal spendAmount, String spendCurrency, String mcc) {
        Map<String, String> md = new HashMap<>();
        md.put("useCase", EventTypes.CC_TXN_LP);
        if (mcc != null && !mcc.isBlank()) {
            md.put("mcc", mcc.trim());
        }
        return events.submit(TransactionalEvent.builder()
            .eventId(require(eventId, "eventId"))
            .userId(require(ownerId, "ownerId"))
            .eventType(EventTypes.CC_TXN_LP)
            .amount(requireAmount(spendAmount))
            .currency(requireCcy(spendCurrency))
            .occurredAt(Instant.now())
            .metadata(md)
            .build());
    }

    /** Classic PURCHASE event (demo grocery path). */
    public IngestionResult purchase(String ownerId, String eventId,
                                    BigDecimal amount, String currency, String mcc) {
        Map<String, String> md = new HashMap<>();
        md.put("useCase", EventTypes.PURCHASE);
        if (mcc != null && !mcc.isBlank()) {
            md.put("mcc", mcc.trim());
        }
        return events.submit(TransactionalEvent.builder()
            .eventId(require(eventId, "eventId"))
            .userId(require(ownerId, "ownerId"))
            .eventType(EventTypes.PURCHASE)
            .amount(requireAmount(amount))
            .currency(requireCcy(currency))
            .occurredAt(Instant.now())
            .metadata(md)
            .build());
    }

    public IngestionResult purchase(String ownerId, String eventId, BigDecimal amount, String currency) {
        return purchase(ownerId, eventId, amount, currency, null);
    }

    /**
     * Escape hatch: typed eventType + amount, still no raw HTTP JSON from caller.
     */
    public IngestionResult earn(String eventType, String ownerId, String eventId,
                                BigDecimal amount, String currency, Map<String, String> metadata) {
        Map<String, String> md = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
        md.putIfAbsent("useCase", eventType);
        return events.submit(TransactionalEvent.builder()
            .eventId(require(eventId, "eventId"))
            .userId(require(ownerId, "ownerId"))
            .eventType(require(eventType, "eventType"))
            .amount(amount == null ? BigDecimal.ZERO : amount)
            .currency(requireCcy(currency == null ? "HKD" : currency))
            .occurredAt(Instant.now())
            .metadata(md)
            .build());
    }

    private static TransactionalEvent engagement(String type, String ownerId, String eventId,
                                                 Map<String, String> md) {
        return TransactionalEvent.builder()
            .eventId(require(eventId, "eventId"))
            .userId(require(ownerId, "ownerId"))
            .eventType(type)
            .amount(BigDecimal.ZERO)
            .currency("HKD")
            .occurredAt(Instant.now())
            .metadata(md)
            .build();
    }

    private static String require(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new LedgerValidationException(name + " is required");
        }
        return v.trim();
    }

    private static BigDecimal requireAmount(BigDecimal a) {
        if (a == null) {
            throw new LedgerValidationException("amount is required");
        }
        if (a.signum() < 0) {
            throw new LedgerValidationException("amount must be >= 0");
        }
        return a;
    }

    private static String requireCcy(String c) {
        String u = require(c, "currency").toUpperCase();
        if (!u.matches("[A-Z]{2,4}")) {
            throw new LedgerValidationException("currency must be 2-4 letters");
        }
        return u;
    }
}
