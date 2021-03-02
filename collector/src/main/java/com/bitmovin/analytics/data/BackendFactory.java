package com.bitmovin.analytics.data;

import android.content.Context;
import android.os.Handler;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.retryBackend.RetryBackend;

public class BackendFactory {

    public Backend createBackend(BitmovinAnalyticsConfig config, Context context) {
        HttpBackend httpBackend = new HttpBackend(config.getConfig(), context);
        if (!config.getConfig().getTryResendDataOnFailedConnection()) {
            return httpBackend;
        }

        return new RetryBackend(httpBackend, new Handler());
    }
}
