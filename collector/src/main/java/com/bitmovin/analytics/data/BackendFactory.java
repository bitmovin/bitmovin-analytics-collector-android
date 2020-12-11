package com.bitmovin.analytics.data;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.retryBackend.RetryBackend;
import android.os.Handler;


public class BackendFactory {

    public Backend createBackend(BitmovinAnalyticsConfig config, Context context) {
        HttpBackend httpBackend = new HttpBackend(config, context);
        if(!config.isResendDataOnHttpTimeout() ){
           return httpBackend;
       }

        return new RetryBackend(httpBackend, new Handler());
    }

//    public Backend createRetrySamplesBackend(BitmovinAnalyticsConfig config, Context context){
//        return new RetryBackend(config.getConfig(), context);
//    }
}
