package com.altech.ledger.sdk.model;

/**
 * Stable eventType codes understood by ledger-engine (Brain + COA + recipe).
 * Upstream should prefer {@link com.altech.ledger.sdk.api.UseCaseApi} over raw strings.
 */
public final class EventTypes {
    private EventTypes() {}

    /** Grocery / generic purchase (RATE formula typical). */
    public static final String PURCHASE = "PURCHASE";

    /** CC spend → LP reward (recipe + FIXED/RATE). */
    public static final String CC_TXN_LP = "CC_TXN_LP";
    public static final String CC_TXN_HKD = "CC_TXN_HKD";

    /** Non-financial: like Facebook page → FIXED LP (e.g. 5). */
    public static final String LIKE_FB_PAGE = "LIKE_FB_PAGE";
    public static final String SOCIAL_LIKE = "SOCIAL_LIKE";
    public static final String FOLLOW_IG = "FOLLOW_IG";
    public static final String WATCH_VIDEO = "WATCH_VIDEO";

    public static final String LOAN_DD_LP = "LOAN_DD_LP";
    public static final String LOAN_DD_HKD = "LOAN_DD_HKD";
}
