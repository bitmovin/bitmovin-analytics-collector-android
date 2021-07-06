package com.bitmovin.analytics.license;

import java.util.Map;

public interface LicenseCallback {
    void configureFeatures(boolean authenticated, Map<String, String> features);

    void authenticationCompleted(boolean success);
}
