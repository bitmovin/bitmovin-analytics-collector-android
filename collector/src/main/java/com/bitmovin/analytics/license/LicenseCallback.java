package com.bitmovin.analytics.license;

public interface LicenseCallback {
    void configureFeatures(boolean authenticated, FeatureConfigs featureConfigs);

    void authenticationCompleted(boolean success);
}
