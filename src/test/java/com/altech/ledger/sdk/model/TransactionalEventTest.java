package com.altech.ledger.sdk.model;

import com.altech.ledger.sdk.LedgerValidationException;
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
            .ownerId("01A81267065")
            .mainAccount("90891234567")
            .eventType("PURCHASE")
            .amount(new BigDecimal("12.50"))
            .currency("LP")
            .occurredAt(Instant.parse("2026-08-06T00:00:00Z"))
            .metadata(Map.of("source", "sdk-test", "mcc", "101"))
            .build();

        String json = JsonSupport.mapper().writeValueAsString(event);
        assertTrue(json.contains("\"eventId\":\"evt-1\""));
        assertTrue(json.contains("\"ownerId\":\"01A81267065\""));
        assertTrue(json.contains("\"mainAccount\":\"90891234567\""));
        assertTrue(json.contains("\"currency\":\"LP\""));

        TransactionalEvent back = JsonSupport.mapper().readValue(json, TransactionalEvent.class);
        assertEquals("01A81267065", back.getOwnerId());
        assertEquals("01A81267065", back.getUserId());
        assertEquals("90891234567", back.getMainAccount());
        assertEquals(0, back.getAmount().compareTo(new BigDecimal("12.50")));
    }

    @Test
    void rejectsNegativeAmount() {
        assertThrows(LedgerValidationException.class, () ->
            TransactionalEvent.builder()
                .eventId("e")
                .userId("u")
                .eventType("PURCHASE")
                .amount(new BigDecimal("-1"))
                .currency("LP")
                .build());
    }

    @Test
    void ignoresUnknownPropertiesFromEngine() throws Exception {
        String json = """
            {"eventId":"e1","userId":"U1","eventType":"PURCHASE","amount":10,"currency":"LP",
             "engineOnlyField":true,"schemaVersion":2}
            """;
        TransactionalEvent e = JsonSupport.mapper().readValue(json, TransactionalEvent.class);
        assertEquals("e1", e.getEventId());
        assertEquals("U1", e.getOwnerId());
        e.validate();
    }
}
