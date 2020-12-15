package com.bitmovin.analytics.data;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.CollectorConfig;
import com.bitmovin.analytics.retryBackend.RetryBackend;
import android.os.Handler;


public class BackendFactory {

    public Backend createBackend(CollectorConfig config, Context context) {
        HttpBackend httpBackend = new HttpBackend(config, context);
        if(!config.getTryResendDataOnFailedConnection() ){
           return httpBackend;
       }

        return new RetryBackend(httpBackend, new Handler());
    }
}
