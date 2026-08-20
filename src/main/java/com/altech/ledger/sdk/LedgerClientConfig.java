package com.altech.ledger.sdk;

import com.altech.ledger.sdk.error.RetryPolicy;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for {@link LedgerClient}.
 * <p>
 * UAfinance typical setup:
 * <pre>
 * LedgerClientConfig.builder()
 *     .baseUrl("https://ledger.uafinance.internal")
 *     .bearerToken(System.getenv("LEDGER_TOKEN"))  // if engine requires auth
 *     .defaultExternalType("uafinance")
 *     .defaultCurrency("LP")
 *     .build();
 * </pre>
 */
public final class LedgerClientConfig {
    private final String baseUrl;
    private final Duration connectTimeout;
    private final Duration httpTimeout;
    private final String transactionsPath;
    private final String transactionsDryRunPath;
    private final String sdkInfoPath;
    private final String walletsPath;
    private final String walletsBatchPath;
    private final String useCasesPath;
    private final String kafkaBootstrapServers;
    private final String kafkaTopic;
    private final Map<String, String> kafkaExtra;
    private final String defaultCurrency;
    private final String defaultExternalType;
    private final String bearerToken;
    private final String apiKey;
    private final String apiKeyHeader;
    private final Map<String, String> defaultHeaders;
    private final boolean sendIdempotencyKey;
    private final boolean sendRequestId;
    private final RetryPolicy retryPolicy;

    private LedgerClientConfig(Builder b) {
        this.baseUrl = trimSlash(Objects.requireNonNull(b.baseUrl, "baseUrl"));
        this.connectTimeout = b.connectTimeout == null ? Duration.ofSeconds(5) : b.connectTimeout;
        this.httpTimeout = b.httpTimeout == null ? Duration.ofSeconds(30) : b.httpTimeout;
        this.transactionsPath = b.transactionsPath;
        this.transactionsDryRunPath = b.transactionsDryRunPath;
        this.sdkInfoPath = b.sdkInfoPath;
        this.walletsPath = b.walletsPath;
        this.walletsBatchPath = b.walletsBatchPath;
        this.useCasesPath = b.useCasesPath;
        this.kafkaBootstrapServers = b.kafkaBootstrapServers;
        this.kafkaTopic = b.kafkaTopic;
        this.kafkaExtra = Map.copyOf(b.kafkaExtra);
        this.defaultCurrency = b.defaultCurrency == null ? "LP" : b.defaultCurrency;
        this.defaultExternalType = b.defaultExternalType;
        this.bearerToken = blankToNull(b.bearerToken);
        this.apiKey = blankToNull(b.apiKey);
        this.apiKeyHeader = b.apiKeyHeader == null || b.apiKeyHeader.isBlank() ? "X-Api-Key" : b.apiKeyHeader;
        this.defaultHeaders = Map.copyOf(b.defaultHeaders);
        this.sendIdempotencyKey = b.sendIdempotencyKey;
        this.sendRequestId = b.sendRequestId;
        this.retryPolicy = b.retryPolicy == null ? RetryPolicy.defaults() : b.retryPolicy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBaseUrl() { return baseUrl; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public Duration getHttpTimeout() { return httpTimeout; }
    public String getTransactionsPath() { return transactionsPath; }
    public String getTransactionsDryRunPath() { return transactionsDryRunPath; }
    public String getSdkInfoPath() { return sdkInfoPath; }
    public String getWalletsPath() { return walletsPath; }
    public String getWalletsBatchPath() { return walletsBatchPath; }
    public String getUseCasesPath() { return useCasesPath; }
    public String getKafkaBootstrapServers() { return kafkaBootstrapServers; }
    public String getKafkaTopic() { return kafkaTopic; }
    public Map<String, String> getKafkaExtra() { return kafkaExtra; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public String getDefaultExternalType() { return defaultExternalType; }
    public String getBearerToken() { return bearerToken; }
    public String getApiKey() { return apiKey; }
    public String getApiKeyHeader() { return apiKeyHeader; }
    public Map<String, String> getDefaultHeaders() { return defaultHeaders; }
    public boolean isSendIdempotencyKey() { return sendIdempotencyKey; }
    public boolean isSendRequestId() { return sendRequestId; }
    public RetryPolicy getRetryPolicy() { return retryPolicy; }

    public String url(String path) {
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v;
    }

    public static final class Builder {
        private String baseUrl = "http://localhost:8080";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration httpTimeout = Duration.ofSeconds(30);
        private String transactionsPath = "/integrations/webhooks/transactions";
        private String transactionsDryRunPath = "/integrations/webhooks/transactions/dry-run";
        private String sdkInfoPath = "/integrations/sdk-info";
        private String walletsPath = "/wallets";
        private String walletsBatchPath = "/wallets/batch";
        private String useCasesPath = "/integrations/use-cases";
        private String kafkaBootstrapServers;
        private String kafkaTopic = "ledger.transaction.events";
        private final Map<String, String> kafkaExtra = new HashMap<>();
        private String defaultCurrency = "LP";
        private String defaultExternalType;
        private String bearerToken;
        private String apiKey;
        private String apiKeyHeader = "X-Api-Key";
        private final Map<String, String> defaultHeaders = new HashMap<>();
        private boolean sendIdempotencyKey = true;
        private boolean sendRequestId = true;
        private RetryPolicy retryPolicy = RetryPolicy.defaults();

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder connectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; return this; }
        public Builder httpTimeout(Duration httpTimeout) { this.httpTimeout = httpTimeout; return this; }
        public Builder transactionsPath(String transactionsPath) { this.transactionsPath = transactionsPath; return this; }
        public Builder transactionsDryRunPath(String transactionsDryRunPath) { this.transactionsDryRunPath = transactionsDryRunPath; return this; }
        public Builder sdkInfoPath(String sdkInfoPath) { this.sdkInfoPath = sdkInfoPath; return this; }
        public Builder walletsPath(String walletsPath) { this.walletsPath = walletsPath; return this; }
        public Builder walletsBatchPath(String walletsBatchPath) { this.walletsBatchPath = walletsBatchPath; return this; }
        public Builder useCasesPath(String useCasesPath) { this.useCasesPath = useCasesPath; return this; }
        public Builder kafkaBootstrapServers(String kafkaBootstrapServers) {
            this.kafkaBootstrapServers = kafkaBootstrapServers; return this;
        }
        public Builder kafkaTopic(String kafkaTopic) { this.kafkaTopic = kafkaTopic; return this; }
        public Builder kafkaProperty(String key, String value) { this.kafkaExtra.put(key, value); return this; }
        public Builder defaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; return this; }

        /** Applied to wallet onboard when request does not set externalType (e.g. {@code "uafinance"}). */
        public Builder defaultExternalType(String defaultExternalType) {
            this.defaultExternalType = defaultExternalType; return this;
        }

        /** {@code Authorization: Bearer <token>} */
        public Builder bearerToken(String bearerToken) { this.bearerToken = bearerToken; return this; }

        /** Custom API key header (default {@code X-Api-Key}). */
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder apiKeyHeader(String apiKeyHeader) { this.apiKeyHeader = apiKeyHeader; return this; }

        public Builder defaultHeader(String name, String value) {
            this.defaultHeaders.put(name, value); return this;
        }

        public Builder sendIdempotencyKey(boolean sendIdempotencyKey) {
            this.sendIdempotencyKey = sendIdempotencyKey; return this;
        }

        public Builder sendRequestId(boolean sendRequestId) {
            this.sendRequestId = sendRequestId; return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy; return this;
        }

        /** Disable automatic REST retries. */
        public Builder noRetries() {
            this.retryPolicy = RetryPolicy.none(); return this;
        }

        public LedgerClientConfig build() {
            return new LedgerClientConfig(this);
        }
    }
}
