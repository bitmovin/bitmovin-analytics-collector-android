package com.bitmovin.analytics.license;

import java.util.Map;

public interface LicenseCallback {
    void configureFeatures(boolean authenticated, FeatureConfigs featureConfigs);

    void authenticationCompleted(boolean success);
}
