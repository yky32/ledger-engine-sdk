package com.altech.ledger.sdk;

/**
 * Transport channel for shooting work into ledger-engine.
 */
public enum DeliveryChannel {
    /** Synchronous HTTP; returns engine processing result. */
    REST,
    /** Async Kafka publish; engine consumes later. */
    KAFKA
}
