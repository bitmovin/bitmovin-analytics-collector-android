package com.bitmovin.analytics.data;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;

public class BackendFactory {

    public Backend createBackend(BitmovinAnalyticsConfig config, Context context) {
        return new HttpBackend(config.getCollectorConfig(), context);
    }
}
