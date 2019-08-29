package com.bitmovin.analytics.license;

public interface LicenseCallback {
    void authenticationCompleted(boolean success);
}