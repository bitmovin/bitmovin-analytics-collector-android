package com.bitmovin.analytics.data;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.retryBackend.RetryBackend;

public class BackendFactory {

    public Backend createBackend(BitmovinAnalyticsConfig config, Context context) {
        return new HttpBackend(config.getConfig(), context);
    }

    public Backend createRetrySamplesBackend(BitmovinAnalyticsConfig config, Context context){
        return new RetryBackend(config.getConfig(), context);
    }
}
