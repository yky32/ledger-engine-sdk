package com.altech.ledger.sdk.rest;

import com.altech.ledger.sdk.LedgerClientConfig;
import com.altech.ledger.sdk.LedgerException;
import com.altech.ledger.sdk.LedgerNetworkException;
import com.altech.ledger.sdk.LedgerValidationException;
import com.altech.ledger.sdk.error.ErrorMapper;
import com.altech.ledger.sdk.error.RetryPolicy;
import com.altech.ledger.sdk.json.JsonSupport;
import com.altech.ledger.sdk.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Channel 1 — REST integration with ledger-engine.
 * <p>
 * Features (Phase A):
 * <ul>
 *   <li>Bearer / API-key auth + default headers</li>
 *   <li>{@code Idempotency-Key} on mutating calls</li>
 *   <li>{@code X-Request-Id} for support correlation</li>
 *   <li>Retry with jitter on 429 / 5xx / network</li>
 *   <li>Typed exceptions from engine {@code ApiError} JSON</li>
 * </ul>
 */
public final class RestLedgerClient implements AutoCloseable {
    private final LedgerClientConfig config;
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final RetryPolicy retryPolicy;

    public RestLedgerClient(LedgerClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.mapper = JsonSupport.mapper();
        this.retryPolicy = config.getRetryPolicy();
        this.http = HttpClient.newBuilder()
            .connectTimeout(config.getConnectTimeout())
            .build();
    }

    /** Phase 1 — create one customer wallet. */
    public WalletOnboardResponse onboardWallet(OnboardWalletRequest request) {
        OnboardWalletRequest req = applyDefaults(request);
        req.validate();
        String idem = "wallet:" + req.getUserId() + ":" + req.getCurrency();
        return post(config.getWalletsPath(), req, WalletOnboardResponse.class, idem, 201, 200);
    }

    /** Phase 1 — batch create (max 1000). */
    public BatchOnboardWalletResponse onboardWalletsBatch(List<OnboardWalletRequest> wallets) {
        if (wallets == null || wallets.isEmpty()) {
            throw new LedgerValidationException("wallets must not be empty");
        }
        if (wallets.size() > 1000) {
            throw new LedgerValidationException("max 1000 wallets per batch");
        }
        List<OnboardWalletRequest> normalized = wallets.stream().map(this::applyDefaults).toList();
        normalized.forEach(OnboardWalletRequest::validate);
        String idem = "wallet-batch:" + UUID.randomUUID();
        return post(config.getWalletsBatchPath(), new BatchOnboardWalletRequest(normalized),
            BatchOnboardWalletResponse.class, idem, 200, 201);
    }

    /** Phase 2 — shoot transactional event; engine applies rules and LP. */
    public IngestionResult ingestTransaction(TransactionalEvent event) {
        event.validate();
        return post(config.getTransactionsPath(), event, IngestionResult.class, event.getEventId(), 200);
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

    private OnboardWalletRequest applyDefaults(OnboardWalletRequest request) {
        Objects.requireNonNull(request, "request");
        if ((request.getCurrency() == null || request.getCurrency().isBlank())
            && config.getDefaultCurrency() != null) {
            request.setCurrency(config.getDefaultCurrency());
        }
        if ((request.getExternalType() == null || request.getExternalType().isBlank())
            && config.getDefaultExternalType() != null) {
            request.setExternalType(config.getDefaultExternalType());
        }
        return request;
    }

    private <T> T post(String path, Object body, Class<T> type, String idempotencyKey, int... okStatuses) {
        try {
            byte[] json = mapper.writeValueAsBytes(body);
            HttpResponse<String> response = sendWithRetry("POST", path, json, idempotencyKey);
            assertOk(response, okStatuses);
            return readBody(response.body(), type);
        } catch (LedgerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LedgerNetworkException("REST POST " + path + " failed: " + ex.getMessage(), ex);
        }
    }

    private <T> T get(String path, Class<T> type) {
        try {
            HttpResponse<String> response = sendWithRetry("GET", path, null, null);
            assertOk(response, 200);
            return readBody(response.body(), type);
        } catch (LedgerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LedgerNetworkException("REST GET " + path + " failed: " + ex.getMessage(), ex);
        }
    }

    private <T> T get(String path, TypeReference<T> type) {
        try {
            HttpResponse<String> response = sendWithRetry("GET", path, null, null);
            assertOk(response, 200);
            return readBody(response.body(), type);
        } catch (LedgerException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new LedgerNetworkException("REST GET " + path + " failed: " + ex.getMessage(), ex);
        }
    }

    /** Supports engine {@code Result} envelope {@code {code,data}} or bare JSON. */
    private <T> T readBody(String body, Class<T> type) throws Exception {
        if (body == null || body.isBlank()) {
            return null;
        }
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(body);
        if (root != null && root.isObject() && root.has("data") && root.has("code")) {
            com.fasterxml.jackson.databind.JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                return null;
            }
            return mapper.treeToValue(data, type);
        }
        return mapper.readValue(body, type);
    }

    private <T> T readBody(String body, TypeReference<T> type) throws Exception {
        if (body == null || body.isBlank()) {
            return null;
        }
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(body);
        if (root != null && root.isObject() && root.has("data") && root.has("code")) {
            com.fasterxml.jackson.databind.JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                return null;
            }
            return mapper.convertValue(data, type);
        }
        return mapper.readValue(body, type);
    }

    /**
     * Ops-configured use-case catalog (Brain + COA + recipe).
     * {@code GET /integrations/use-cases?enabledOnly=true}
     */
    public List<UseCaseDescriptor> listUseCases(boolean enabledOnly) {
        String path = config.getUseCasesPath() + "?enabledOnly=" + enabledOnly;
        List<UseCaseDescriptor> list = get(path, new TypeReference<>() {});
        return list == null ? List.of() : list;
    }

    public UseCaseDescriptor getUseCase(String code) {
        return get(config.getUseCasesPath() + "/" + encode(code), UseCaseDescriptor.class);
    }

    private HttpResponse<String> sendWithRetry(String method, String path, byte[] body,
                                               String idempotencyKey) {
        int attempt = 0;
        while (true) {
            String requestId = config.isSendRequestId() ? UUID.randomUUID().toString() : null;
            try {
                HttpRequest request = buildRequest(method, path, body, idempotencyKey, requestId);
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (retryPolicy.shouldRetry(attempt, status)) {
                    // Prefer Retry-After for 429 when present
                    if (status == 429) {
                        String ra = response.headers().firstValue("Retry-After").orElse(null);
                        if (ra != null) {
                            try {
                                long sec = Long.parseLong(ra.trim());
                                if (sec > 0) {
                                    Thread.sleep(Math.min(sec, 30) * 1000L);
                                }
                            } catch (NumberFormatException | InterruptedException e) {
                                if (e instanceof InterruptedException) {
                                    Thread.currentThread().interrupt();
                                    throw new LedgerNetworkException("Retry interrupted", e);
                                }
                                retryPolicy.sleep(attempt);
                            }
                        } else {
                            retryPolicy.sleep(attempt);
                        }
                    } else {
                        retryPolicy.sleep(attempt);
                    }
                    attempt++;
                    continue;
                }
                return response;
            } catch (IOException ex) {
                if (retryPolicy.shouldRetryNetwork(attempt)) {
                    retryPolicy.sleep(attempt);
                    attempt++;
                    continue;
                }
                throw new LedgerNetworkException(
                    "REST " + method + " " + path + " network failure after " + (attempt + 1) + " attempt(s): "
                        + ex.getMessage(), ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new LedgerNetworkException("REST " + method + " " + path + " interrupted", ex);
            }
        }
    }

    private HttpRequest buildRequest(String method, String path, byte[] body,
                                     String idempotencyKey, String requestId) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
            .uri(URI.create(config.url(path)))
            .timeout(config.getHttpTimeout())
            .header("Accept", "application/json");

        if (config.getBearerToken() != null) {
            b.header("Authorization", "Bearer " + config.getBearerToken());
        }
        if (config.getApiKey() != null) {
            b.header(config.getApiKeyHeader(), config.getApiKey());
        }
        config.getDefaultHeaders().forEach(b::header);

        if (requestId != null) {
            b.header("X-Request-Id", requestId);
        }
        if (config.isSendIdempotencyKey() && idempotencyKey != null && !idempotencyKey.isBlank()
            && ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
            b.header("Idempotency-Key", idempotencyKey);
        }

        if ("GET".equals(method)) {
            b.GET();
        } else if ("POST".equals(method)) {
            b.header("Content-Type", "application/json");
            b.POST(HttpRequest.BodyPublishers.ofByteArray(body == null ? new byte[0] : body));
        } else {
            throw new IllegalArgumentException("Unsupported method: " + method);
        }
        return b.build();
    }

    private static void assertOk(HttpResponse<String> response, int... okStatuses) {
        int code = response.statusCode();
        for (int ok : okStatuses) {
            if (code == ok) return;
        }
        String requestId = response.headers().firstValue("X-Request-Id")
            .or(() -> response.headers().firstValue("X-Request-ID"))
            .orElse(null);
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
        throw ErrorMapper.fromHttp(code, response.body(), requestId, retryAfter);
    }

    private static String encode(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        // HttpClient does not require close
    }
}
