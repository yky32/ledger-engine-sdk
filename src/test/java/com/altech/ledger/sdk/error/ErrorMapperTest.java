package com.altech.ledger.sdk.error;

import com.altech.ledger.sdk.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ErrorMapperTest {

    @Test
    void mapsValidationWithFieldErrors() {
        String body = """
            {
              "timestamp": "2026-08-06T00:00:00Z",
              "status": 400,
              "code": "VALIDATION_FAILED",
              "message": "Request validation failed",
              "path": "/wallets",
              "fieldErrors": { "userId": "must not be blank" }
            }
            """;
        LedgerException ex = ErrorMapper.fromHttp(400, body, "req-1", null);
        assertInstanceOf(LedgerValidationException.class, ex);
        assertEquals("VALIDATION_FAILED", ex.getCode());
        assertEquals("req-1", ex.getRequestId());
        assertEquals("must not be blank", ((LedgerValidationException) ex).getFieldErrors().get("userId"));
        assertNotNull(ex.getApiError());
        assertEquals("/wallets", ex.getPath());
    }

    @Test
    void mapsNotFoundConflictAuthRateLimitServer() {
        assertInstanceOf(LedgerNotFoundException.class,
            ErrorMapper.fromHttp(404, "{\"code\":\"NOT_FOUND\",\"message\":\"gone\"}", null, null));
        assertInstanceOf(LedgerConflictException.class,
            ErrorMapper.fromHttp(409, "{\"code\":\"DATA_CONFLICT\",\"message\":\"dup\"}", null, null));
        assertInstanceOf(LedgerAuthException.class,
            ErrorMapper.fromHttp(401, "{\"code\":\"UNAUTHORIZED\",\"message\":\"nope\"}", null, null));
        assertInstanceOf(LedgerAuthException.class,
            ErrorMapper.fromHttp(403, null, null, null));

        LedgerException rate = ErrorMapper.fromHttp(429, "{\"code\":\"RATE_LIMITED\",\"message\":\"slow\"}", null, "2");
        assertInstanceOf(LedgerRateLimitException.class, rate);
        assertTrue(rate.isRetryable());
        assertEquals(Duration.ofSeconds(2), ((LedgerRateLimitException) rate).getRetryAfter());

        LedgerException server = ErrorMapper.fromHttp(503, "{\"code\":\"SERVER_ERROR\",\"message\":\"down\"}", null, null);
        assertInstanceOf(LedgerServerException.class, server);
        assertTrue(server.isRetryable());
    }

    @Test
    void toleratesNonJsonBody() {
        LedgerException ex = ErrorMapper.fromHttp(500, "plain text boom", "r2", null);
        assertInstanceOf(LedgerServerException.class, ex);
        assertEquals("SERVER_ERROR", ex.getCode());
        assertTrue(ex.getMessage().contains("Unexpected HTTP 500") || ex.getMessage().contains("500"));
    }
}
