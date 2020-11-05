package com.bitmovin.analytics.license;

import com.bitmovin.analytics.data.AdEventData;
import com.bitmovin.analytics.data.EventData;

import java.util.Collection;
import java.util.Map;

public interface LicenseCallback {
    void configureFeatures(boolean authenticated, Map<String, String> settings, Collection<EventData> samples, Collection<AdEventData> adSamples);
    void authenticationCompleted(boolean success);
}
