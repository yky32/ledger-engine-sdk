package com.altech.ledger.sdk;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for {@link LedgerClient}.
 */
public final class LedgerClientConfig {
    private final String baseUrl;
    private final Duration httpTimeout;
    private final String transactionsPath;
    private final String walletsPath;
    private final String walletsBatchPath;
    private final String kafkaBootstrapServers;
    private final String kafkaTopic;
    private final Map<String, String> kafkaExtra;
    private final String defaultCurrency;

    private LedgerClientConfig(Builder b) {
        this.baseUrl = trimSlash(Objects.requireNonNull(b.baseUrl, "baseUrl"));
        this.httpTimeout = b.httpTimeout == null ? Duration.ofSeconds(30) : b.httpTimeout;
        this.transactionsPath = b.transactionsPath;
        this.walletsPath = b.walletsPath;
        this.walletsBatchPath = b.walletsBatchPath;
        this.kafkaBootstrapServers = b.kafkaBootstrapServers;
        this.kafkaTopic = b.kafkaTopic;
        this.kafkaExtra = Map.copyOf(b.kafkaExtra);
        this.defaultCurrency = b.defaultCurrency == null ? "LP" : b.defaultCurrency;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBaseUrl() { return baseUrl; }
    public Duration getHttpTimeout() { return httpTimeout; }
    public String getTransactionsPath() { return transactionsPath; }
    public String getWalletsPath() { return walletsPath; }
    public String getWalletsBatchPath() { return walletsBatchPath; }
    public String getKafkaBootstrapServers() { return kafkaBootstrapServers; }
    public String getKafkaTopic() { return kafkaTopic; }
    public Map<String, String> getKafkaExtra() { return kafkaExtra; }
    public String getDefaultCurrency() { return defaultCurrency; }

    public String url(String path) {
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static final class Builder {
        private String baseUrl = "http://localhost:8080";
        private Duration httpTimeout = Duration.ofSeconds(30);
        private String transactionsPath = "/integrations/webhooks/transactions";
        private String walletsPath = "/wallets";
        private String walletsBatchPath = "/wallets/batch";
        private String kafkaBootstrapServers;
        private String kafkaTopic = "ledger.transaction.events";
        private final Map<String, String> kafkaExtra = new HashMap<>();
        private String defaultCurrency = "LP";

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder httpTimeout(Duration httpTimeout) { this.httpTimeout = httpTimeout; return this; }
        public Builder transactionsPath(String transactionsPath) { this.transactionsPath = transactionsPath; return this; }
        public Builder walletsPath(String walletsPath) { this.walletsPath = walletsPath; return this; }
        public Builder walletsBatchPath(String walletsBatchPath) { this.walletsBatchPath = walletsBatchPath; return this; }
        public Builder kafkaBootstrapServers(String kafkaBootstrapServers) {
            this.kafkaBootstrapServers = kafkaBootstrapServers; return this;
        }
        public Builder kafkaTopic(String kafkaTopic) { this.kafkaTopic = kafkaTopic; return this; }
        public Builder kafkaProperty(String key, String value) { this.kafkaExtra.put(key, value); return this; }
        public Builder defaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; return this; }

        public LedgerClientConfig build() {
            return new LedgerClientConfig(this);
        }
    }
}
