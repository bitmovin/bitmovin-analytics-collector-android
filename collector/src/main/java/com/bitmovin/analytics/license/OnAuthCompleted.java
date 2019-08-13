package com.bitmovin.analytics.license;

public interface OnAuthCompleted {
    void authenticationCompleted(boolean success, String key);
}