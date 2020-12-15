package com.bitmovin.analytics.utils

import android.util.Log
import com.bitmovin.analytics.CollectorConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

class ClientFactory {
    fun createClient(config: CollectorConfig): OkHttpClient {
        return if (config.tryResendDataOnFailedConnection) {
            Log.d("RetryBackend", "retry")
            OkHttpClient.Builder()
                    .retryOnConnectionFailure(false)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .build()
        } else {
            Log.d("RetryBackend", "regular")
            OkHttpClient()
        }
    }
}
