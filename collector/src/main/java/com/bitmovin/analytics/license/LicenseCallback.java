package com.bitmovin.analytics.license;

import java.util.Map;

public interface LicenseCallback {
    void configureFeatures(boolean authenticated, Map<String, String> settings);

    void authenticationCompleted(boolean success);
}
