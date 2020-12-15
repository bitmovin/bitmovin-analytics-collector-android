package com.bitmovin.analytics.utils

import android.util.Log
import com.bitmovin.analytics.CollectorConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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
