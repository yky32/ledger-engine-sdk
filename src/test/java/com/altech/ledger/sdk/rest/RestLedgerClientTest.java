package com.altech.ledger.sdk.rest;

import com.altech.ledger.sdk.*;
import com.altech.ledger.sdk.error.RetryPolicy;
import com.altech.ledger.sdk.model.IngestionResult;
import com.altech.ledger.sdk.model.OnboardWalletRequest;
import com.altech.ledger.sdk.model.TransactionalEvent;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RestLedgerClientTest {

    private HttpServer server;
    private String baseUrl;
    private final List<String> capturedAuth = new CopyOnWriteArrayList<>();
    private final List<String> capturedIdem = new CopyOnWriteArrayList<>();
    private final List<String> capturedRequestIds = new CopyOnWriteArrayList<>();
    private final AtomicInteger hitCount = new AtomicInteger();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsAuthIdempotencyAndRequestId() {
        server.createContext("/integrations/webhooks/transactions", exchange -> {
            hitCount.incrementAndGet();
            capturedAuth.add(exchange.getRequestHeaders().getFirst("Authorization"));
            capturedIdem.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            capturedRequestIds.add(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            byte[] body = """
                {"eventId":"pos-1","status":"EARNED","operation":"EARN","points":10}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        LedgerClientConfig config = LedgerClientConfig.builder()
            .baseUrl(baseUrl)
            .bearerToken("uaf-secret")
            .retryPolicy(RetryPolicy.none())
            .build();

        try (RestLedgerClient client = new RestLedgerClient(config)) {
            IngestionResult result = client.ingestTransaction(TransactionalEvent.builder()
                .eventId("pos-1")
                .userId("UAF-1")
                .eventType("PURCHASE")
                .amount(new BigDecimal("100"))
                .currency("LP")
                .build());
            assertEquals(IngestionResult.Status.EARNED, result.getStatus());
            assertEquals(0, result.getPoints().compareTo(new BigDecimal("10")));
        }

        assertEquals("Bearer uaf-secret", capturedAuth.get(0));
        assertEquals("pos-1", capturedIdem.get(0));
        assertNotNull(capturedRequestIds.get(0));
        assertFalse(capturedRequestIds.get(0).isBlank());
    }

    @Test
    void appliesUafinanceExternalTypeDefault() {
        server.createContext("/wallets", exchange -> {
            String reqBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(reqBody.contains("\"externalType\":\"uafinance\""));
            assertTrue(reqBody.contains("\"currency\":\"LP\""));
            byte[] body = """
                {"walletId":1,"ownerId":"UAF-1","currency":"LP","status":"ACTIVE","externalType":"uafinance"}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        try (RestLedgerClient client = new RestLedgerClient(LedgerClientConfig.builder()
            .baseUrl(baseUrl)
            .defaultExternalType("uafinance")
            .defaultCurrency("LP")
            .retryPolicy(RetryPolicy.none())
            .build())) {
            var resp = client.onboardWallet(OnboardWalletRequest.builder()
                .userId("UAF-1")
                .build());
            assertEquals("UAF-1", resp.getOwnerId());
            assertEquals("uafinance", resp.getExternalType());
        }
    }

    @Test
    void mapsEngineErrorToTypedException() {
        server.createContext("/wallets/missing/LP", exchange -> {
            byte[] body = """
                {"timestamp":"2026-08-06T00:00:00Z","status":404,"code":"NOT_FOUND",
                 "message":"Wallet not found","path":"/wallets/missing/LP","fieldErrors":{}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("X-Request-Id", "engine-req-9");
            exchange.sendResponseHeaders(404, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        try (RestLedgerClient client = new RestLedgerClient(LedgerClientConfig.builder()
            .baseUrl(baseUrl)
            .retryPolicy(RetryPolicy.none())
            .build())) {
            LedgerNotFoundException ex = assertThrows(LedgerNotFoundException.class,
                () -> client.getWallet("missing", "LP"));
            assertEquals("NOT_FOUND", ex.getCode());
            assertEquals("Wallet not found", ex.getApiError().getMessage());
        }
    }

    @Test
    void retriesOn503ThenSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/integrations/webhooks/transactions", exchange -> {
            int n = calls.incrementAndGet();
            if (n == 1) {
                byte[] body = "{\"status\":503,\"code\":\"SERVER_ERROR\",\"message\":\"busy\"}"
                    .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(503, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
                return;
            }
            byte[] body = """
                {"eventId":"e-retry","status":"EARNED","points":1}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        try (RestLedgerClient client = new RestLedgerClient(LedgerClientConfig.builder()
            .baseUrl(baseUrl)
            .retryPolicy(new RetryPolicy(3, Duration.ofMillis(10), Duration.ofMillis(50)))
            .build())) {
            IngestionResult result = client.ingestTransaction(TransactionalEvent.builder()
                .eventId("e-retry")
                .userId("UAF-1")
                .eventType("PURCHASE")
                .amount(new BigDecimal("1"))
                .currency("LP")
                .build());
            assertEquals(IngestionResult.Status.EARNED, result.getStatus());
        }
        assertEquals(2, calls.get());
    }

    @Test
    void ignoresUnknownJsonFieldsOnResponse() {
        server.createContext("/integrations/webhooks/transactions", exchange -> {
            byte[] body = """
                {"eventId":"e2","status":"EARNED","points":5,"futureField":"ok","nested":{"x":1}}
                """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        try (RestLedgerClient client = new RestLedgerClient(LedgerClientConfig.builder()
            .baseUrl(baseUrl)
            .retryPolicy(RetryPolicy.none())
            .build())) {
            IngestionResult result = client.ingestTransaction(TransactionalEvent.builder()
                .eventId("e2")
                .userId("UAF-1")
                .eventType("PURCHASE")
                .amount(new BigDecimal("5"))
                .currency("LP")
                .build());
            assertEquals("e2", result.getEventId());
        }
    }
}
