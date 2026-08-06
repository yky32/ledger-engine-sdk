package com.altech.ledger.sdk.api;

import com.altech.ledger.sdk.DeliveryChannel;
import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.LedgerValidationException;
import com.altech.ledger.sdk.batch.BatchOptions;
import com.altech.ledger.sdk.batch.BatchResult;
import com.altech.ledger.sdk.error.RetryPolicy;
import com.altech.ledger.sdk.model.IngestionResult;
import com.altech.ledger.sdk.model.TransactionalEvent;
import com.altech.ledger.sdk.rest.RestLedgerClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EventApiBatchTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger hits = new AtomicInteger();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/integrations/webhooks/transactions", exchange -> {
            int n = hits.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            if (n == 1) {
                byte[] resp = """
                    {"status":400,"code":"VALIDATION_FAILED","message":"nope"}
                    """.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
                return;
            }
            byte[] resp = "{\"eventId\":\"ok\",\"status\":\"EARNED\",\"points\":1}"
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void continueOnErrorCollectsFailures() {
        EventApi api = new EventApi(
            new RestLedgerClient(LedgerClientConfig.builder()
                .baseUrl(baseUrl)
                .retryPolicy(RetryPolicy.none())
                .build()),
            () -> { throw new IllegalStateException("no kafka"); },
            ForkJoinPool.commonPool());

        List<TransactionalEvent> events = List.of(
            event("bad"),
            event("good")
        );

        BatchResult<IngestionResult> batch = api.submitBatch(events, DeliveryChannel.REST,
            BatchOptions.continueOnError());

        assertEquals(2, batch.size());
        assertEquals(1, batch.failureCount());
        assertEquals(1, batch.successCount());
        assertEquals(2, hits.get());
    }

    @Test
    void failFastStops() {
        EventApi api = new EventApi(
            new RestLedgerClient(LedgerClientConfig.builder()
                .baseUrl(baseUrl)
                .retryPolicy(RetryPolicy.none())
                .build()),
            () -> { throw new IllegalStateException("no kafka"); },
            ForkJoinPool.commonPool());

        assertThrows(LedgerValidationException.class, () ->
            api.submitBatch(List.of(event("bad"), event("good")), DeliveryChannel.REST,
                BatchOptions.failFast()));
        assertEquals(1, hits.get());
    }

    private static TransactionalEvent event(String id) {
        return TransactionalEvent.builder()
            .eventId(id)
            .userId("U1")
            .eventType("PURCHASE")
            .amount(new BigDecimal("1"))
            .currency("LP")
            .build();
    }
}
