package com.altech.ledger.sdk.api;

import com.altech.ledger.sdk.DeliveryChannel;
import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.batch.BatchOptions;
import com.altech.ledger.sdk.batch.BatchResult;
import com.altech.ledger.sdk.batch.ItemOutcome;
import com.altech.ledger.sdk.error.RetryPolicy;
import com.altech.ledger.sdk.model.IngestionResult;
import com.altech.ledger.sdk.rest.RestLedgerClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FileApiStreamingTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger hits = new AtomicInteger();

    @TempDir
    Path dir;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/integrations/webhooks/transactions", exchange -> {
            int n = hits.incrementAndGet();
            String body = exchange.getRequestBody().readAllBytes().length > 0
                ? new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
                : "";
            // re-read won't work — already consumed. Use status by hit count only.
            byte[] resp;
            int code = 200;
            if (n == 2) {
                // second event fails
                resp = """
                    {"timestamp":"2026-08-06T00:00:00Z","status":400,"code":"VALIDATION_FAILED",
                     "message":"bad event","path":"/integrations/webhooks/transactions","fieldErrors":{}}
                    """.getBytes(StandardCharsets.UTF_8);
                code = 400;
            } else {
                resp = ("{\"eventId\":\"e" + n + "\",\"status\":\"EARNED\",\"points\":1}")
                    .getBytes(StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, resp.length);
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
    void streamsNdjsonContinueOnError() throws Exception {
        Path ndjson = dir.resolve("events.ndjson");
        Files.writeString(ndjson, """
            {"eventId":"e1","userId":"U1","eventType":"PURCHASE","amount":10,"currency":"LP"}
            {"eventId":"e2","userId":"U2","eventType":"PURCHASE","amount":20,"currency":"LP"}
            {"eventId":"e3","userId":"U3","eventType":"PURCHASE","amount":30,"currency":"LP"}
            """);

        FileApi api = new FileApi(
            new RestLedgerClient(LedgerClientConfig.builder()
                .baseUrl(baseUrl)
                .retryPolicy(RetryPolicy.none())
                .build()),
            () -> { throw new IllegalStateException("no kafka"); });

        BatchResult<IngestionResult> batch = api.process(ndjson, DeliveryChannel.REST,
            BatchOptions.continueOnError());

        assertEquals(3, batch.size());
        assertEquals(2, batch.successCount());
        assertEquals(1, batch.failureCount());
        assertEquals(ItemOutcome.FAILURE, batch.getItems().get(1).getOutcome());
        assertEquals(3, hits.get());
    }

    @Test
    void streamsJsonArray() throws Exception {
        hits.set(0);
        // reset handler to always succeed
        server.removeContext("/integrations/webhooks/transactions");
        server.createContext("/integrations/webhooks/transactions", exchange -> {
            hits.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            byte[] resp = "{\"eventId\":\"x\",\"status\":\"EARNED\",\"points\":1}"
                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        Path arr = dir.resolve("events.json");
        Files.writeString(arr, """
            [
              {"eventId":"a1","userId":"U1","eventType":"PURCHASE","amount":1,"currency":"LP"},
              {"eventId":"a2","userId":"U2","eventType":"PURCHASE","amount":2,"currency":"LP"}
            ]
            """);

        FileApi api = new FileApi(
            new RestLedgerClient(LedgerClientConfig.builder()
                .baseUrl(baseUrl)
                .retryPolicy(RetryPolicy.none())
                .build()),
            () -> { throw new IllegalStateException("no kafka"); });

        BatchResult<IngestionResult> batch = api.process(arr, DeliveryChannel.REST, BatchOptions.failFast());
        assertEquals(2, batch.successCount());
        assertEquals(2, hits.get());
    }
}
