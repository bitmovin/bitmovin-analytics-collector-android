package com.bitmovin.analytics.license;

public interface LicenseCallback {
    void configureFeatures(boolean authenticated, FeatureConfigContainer featureConfigs);

    void authenticationCompleted(boolean success);
}
