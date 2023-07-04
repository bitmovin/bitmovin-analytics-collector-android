package com.bitmovin.analytics.utils

import com.bitmovin.analytics.api.AnalyticsConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ClientFactory {
    fun createClient(config: AnalyticsConfig): OkHttpClient {
        // if inMemoryRetryEnabled is true we disable the retry from
        // okhttp and handle it ourselves
        return if (config.inMemoryRetryEnabled) {
            OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .connectTimeout(15, TimeUnit.SECONDS)
                .build()
        } else {
            OkHttpClient()
        }
    }
}
