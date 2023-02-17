package com.bitmovin.analytics.data

import android.content.Context
import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.retryBackend.RetryBackend

class BackendFactory {
    fun createBackend(config: BitmovinAnalyticsConfig, context: Context): Backend {
        val httpBackend = HttpBackend(config.config, context)
        return if (!config.config.tryResendDataOnFailedConnection) {
            httpBackend
        } else {
            RetryBackend(httpBackend, Handler())
        }
    }
}
