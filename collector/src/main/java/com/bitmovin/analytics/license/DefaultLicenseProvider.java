package com.bitmovin.analytics.license;

public class DefaultLicenseProvider implements OnAuthCompleted, LicenseProvider {
    private String analyticsLicense;

    @Override
    public void authenticationCompleted(boolean success, String key) {
        if (success){
            this.analyticsLicense = key;
        }
    }

    @Override
    public String getAnalyticsLicense() {
        return analyticsLicense;
    }
}
