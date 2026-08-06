package com.altech.ledger.sdk.rest;

import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.json.JsonSupport;
import com.altech.ledger.sdk.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Channel 1 — REST integration with ledger-engine.
 */
public final class RestLedgerClient implements AutoCloseable {
    private final LedgerClientConfig config;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public RestLedgerClient(LedgerClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.mapper = JsonSupport.mapper();
        this.http = HttpClient.newBuilder()
            .connectTimeout(config.getHttpTimeout())
            .build();
    }

    /** Phase 1 — create one customer wallet. */
    public WalletOnboardResponse onboardWallet(OnboardWalletRequest request) {
        request.validate();
        return post(config.getWalletsPath(), request, WalletOnboardResponse.class, 201, 200);
    }

    /** Phase 1 — batch create (max 1000). */
    public BatchOnboardWalletResponse onboardWalletsBatch(List<OnboardWalletRequest> wallets) {
        if (wallets == null || wallets.isEmpty()) {
            throw new IllegalArgumentException("wallets must not be empty");
        }
        if (wallets.size() > 1000) {
            throw new IllegalArgumentException("max 1000 wallets per batch");
        }
        wallets.forEach(OnboardWalletRequest::validate);
        return post(config.getWalletsBatchPath(), new BatchOnboardWalletRequest(wallets),
            BatchOnboardWalletResponse.class, 200, 201);
    }

    /** Phase 2 — shoot transactional event; engine applies rules and LP. */
    public IngestionResult ingestTransaction(TransactionalEvent event) {
        event.validate();
        return post(config.getTransactionsPath(), event, IngestionResult.class, 200);
    }

    /** Lookup wallet by owner + currency. */
    public WalletOnboardResponse getWallet(String ownerId, String currency) {
        String path = config.getWalletsPath() + "/"
            + encode(ownerId) + "/" + encode(currency);
        return get(path, WalletOnboardResponse.class);
    }

    /** List wallets for owner. */
    public List<WalletOnboardResponse> listWallets(String ownerId) {
        String path = config.getWalletsPath() + "?ownerId=" + encode(ownerId);
        return get(path, new TypeReference<>() {});
    }

    private <T> T post(String path, Object body, Class<T> type, int... okStatuses) {
        try {
            byte[] json = mapper.writeValueAsBytes(body);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.url(path)))
                .timeout(config.getHttpTimeout())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json))
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            assertOk(response, okStatuses);
            if (response.body() == null || response.body().isBlank()) {
                return null;
            }
            return mapper.readValue(response.body(), type);
        } catch (LedgerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LedgerException("REST POST " + path + " failed: " + ex.getMessage(), ex);
        }
    }

    private <T> T get(String path, Class<T> type) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.url(path)))
                .timeout(config.getHttpTimeout())
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            assertOk(response, 200);
            return mapper.readValue(response.body(), type);
        } catch (LedgerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LedgerException("REST GET " + path + " failed: " + ex.getMessage(), ex);
        }
    }

    private <T> T get(String path, TypeReference<T> type) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.url(path)))
                .timeout(config.getHttpTimeout())
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            assertOk(response, 200);
            return mapper.readValue(response.body(), type);
        } catch (LedgerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LedgerException("REST GET " + path + " failed: " + ex.getMessage(), ex);
        }
    }

    private static void assertOk(HttpResponse<String> response, int... okStatuses) {
        int code = response.statusCode();
        for (int ok : okStatuses) {
            if (code == ok) return;
        }
        throw new LedgerException(code, "Unexpected HTTP " + code, response.body());
    }

    private static String encode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        // HttpClient does not require close
    }
}
