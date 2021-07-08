package com.bitmovin.analytics.data;

import com.bitmovin.analytics.license.FeatureConfigs;

public class LicenseResponse {
    private String status;
    private String message;
    private FeatureConfigs features;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public FeatureConfigs getFeatures() {
        return features;
    }

    public void setFeatures(FeatureConfigs features) {
        this.features = features;
    }
}
