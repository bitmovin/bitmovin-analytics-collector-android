package com.bitmovin.analytics.data;

import androidx.annotation.Keep;

@Keep // Protect from obfuscation in case customers are using proguard
public class LicenseCallData {
    private String key;
    private String analyticsVersion;
    private String domain;

    public LicenseCallData() {}

    public void setKey(String key) {
        this.key = key;
    }

    public void setAnalyticsVersion(String analyticsVersion) {
        this.analyticsVersion = analyticsVersion;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getKey() {
        return key;
    }
}
