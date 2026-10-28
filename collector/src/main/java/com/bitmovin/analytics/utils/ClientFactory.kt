package com.bitmovin.analytics.utils

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.RetryPolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class ClientFactory {
    fun createClient(config: AnalyticsConfig): OkHttpClient {
        // if retry policy is SHORT_TERM we disable the retry from
        // okhttp and handle it ourselves
        return if (config.retryPolicy == RetryPolicy.SHORT_TERM) {
            OkHttpClient
                .Builder()
                .retryOnConnectionFailure(false)
                .connectTimeout(15, TimeUnit.SECONDS)
                .build()
        } else {
            OkHttpClient()
        }
    }

    internal fun createLicenseClient(
        config: AnalyticsConfig,
        callTimeoutMillis: Long = LICENSE_CALL_TIMEOUT_MILLIS,
    ): OkHttpClient =
        createClient(config)
            .newBuilder()
            .callTimeout(callTimeoutMillis, TimeUnit.MILLISECONDS)
            .build()
}

// same as the Web collector's license call timeout and the iOS request timeout
private const val LICENSE_CALL_TIMEOUT_MILLIS = 60_000L
