package com.altech.ledger.sdk.model;

import com.altech.ledger.sdk.json.JsonSupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransactionalEventTest {

    @Test
    void builderAndJsonRoundTrip() throws Exception {
        TransactionalEvent event = TransactionalEvent.builder()
            .eventId("evt-1")
            .userId("UAF-1")
            .eventType("PURCHASE")
            .amount(new BigDecimal("12.50"))
            .currency("LP")
            .occurredAt(Instant.parse("2026-08-06T00:00:00Z"))
            .metadata(Map.of("source", "sdk-test"))
            .build();

        String json = JsonSupport.mapper().writeValueAsString(event);
        assertTrue(json.contains("\"eventId\":\"evt-1\""));
        assertTrue(json.contains("\"currency\":\"LP\""));

        TransactionalEvent back = JsonSupport.mapper().readValue(json, TransactionalEvent.class);
        assertEquals("UAF-1", back.getUserId());
        assertEquals(0, back.getAmount().compareTo(new BigDecimal("12.50")));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () ->
            TransactionalEvent.builder()
                .eventId("e")
                .userId("u")
                .eventType("PURCHASE")
                .amount(-1)
                .currency("LP")
                .build());
    }
}
