package com.bitmovin.analytics.license;

public interface OnLicenseValidated {
    void validationCompleted(boolean success, String key);
}