package com.bitmovin.analytics.license;

public class DefaultLicenseProvider implements OnLicenseValidated, LicenseProvider {
    private String analyticsLicense;

    @Override
    public void validationCompleted(boolean success, String key) {
        if (success){
            this.analyticsLicense = key;
        }
    }

    @Override
    public String getAnalyticsLicense() {
        return analyticsLicense;
    }
}
