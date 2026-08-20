package com.altech.ledger.sdk.api;

import com.altech.ledger.sdk.model.EventTypes;
import com.altech.ledger.sdk.model.TransactionalEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UseCaseApiBuildTest {

    @Test
    void likeFacebookPageBuildsZeroAmountContract() {
        TransactionalEvent e = TransactionalEvent.builder()
            .eventId("like-1")
            .ownerId("CUST-1")
            .eventType(EventTypes.LIKE_FB_PAGE)
            .amount(BigDecimal.ZERO)
            .currency("HKD")
            .build();
        assertEquals("CUST-1", e.getUserId());
        assertEquals("CUST-1", e.getOwnerId());
        assertEquals(0, e.getAmount().signum());
        assertEquals(EventTypes.LIKE_FB_PAGE, e.getEventType());
    }

    @Test
    void ownerIdAliasOnBuilder() {
        TransactionalEvent e = TransactionalEvent.builder()
            .eventId("e1")
            .ownerId("O1")
            .eventType(EventTypes.PURCHASE)
            .amount(new BigDecimal("10"))
            .currency("HKD")
            .build();
        assertEquals("O1", e.getUserId());
    }
}
