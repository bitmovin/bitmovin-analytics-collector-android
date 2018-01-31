package com.bitmovin.bitmovinanalyticscollector.data;


public class LicenseCallData {
    private String key;
    private String analyticsVersion;
    private String domain;

    public LicenseCallData() {
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setAnalyticsVersion(String analyticsVersion) {
        this.analyticsVersion = analyticsVersion;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }


}

