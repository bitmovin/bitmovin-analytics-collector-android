package com.bitmovin.analytics.data;

import androidx.annotation.Keep;

import com.bitmovin.analytics.license.FeatureConfigContainer;

@Keep // Protect from obfuscation in case customers are using proguard
public class LicenseResponse {
    private String status;
    private String message;
    private FeatureConfigContainer features;

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

    public FeatureConfigContainer getFeatures() {
        return features;
    }

    public void setFeatures(FeatureConfigContainer features) {
        this.features = features;
    }
}
