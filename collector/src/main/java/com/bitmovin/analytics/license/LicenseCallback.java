package com.bitmovin.analytics.license;

import java.util.Map;

public interface LicenseCallback {
    void authenticationCompleted(boolean success, Map<String, String> features);
}
