package com.altech.ledger.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/** Engine handshake from {@code GET /integrations/sdk-info}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SdkInfo {
    private String engineVersion;
    private String product;
    private String minSdkVersion;
    private String recommendedSdkVersion;
    private Map<String, Boolean> features;

    public String getEngineVersion() { return engineVersion; }
    public void setEngineVersion(String engineVersion) { this.engineVersion = engineVersion; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public String getMinSdkVersion() { return minSdkVersion; }
    public void setMinSdkVersion(String minSdkVersion) { this.minSdkVersion = minSdkVersion; }
    public String getRecommendedSdkVersion() { return recommendedSdkVersion; }
    public void setRecommendedSdkVersion(String recommendedSdkVersion) {
        this.recommendedSdkVersion = recommendedSdkVersion;
    }
    public Map<String, Boolean> getFeatures() { return features; }
    public void setFeatures(Map<String, Boolean> features) { this.features = features; }

    public boolean hasFeature(String name) {
        return features != null && Boolean.TRUE.equals(features.get(name));
    }

    @Override
    public String toString() {
        return "SdkInfo{product=" + product + ", engine=" + engineVersion
            + ", minSdk=" + minSdkVersion + "}";
    }
}
