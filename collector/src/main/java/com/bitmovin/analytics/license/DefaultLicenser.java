package com.bitmovin.analytics.license;

public class DefaultLicenser implements OnAuthCompleted, Licenser {
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
