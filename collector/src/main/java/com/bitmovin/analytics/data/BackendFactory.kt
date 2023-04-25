package com.bitmovin.analytics.data

import android.content.Context
import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.persistence.PersistentCacheBackend
import com.bitmovin.analytics.persistence.ReadOnlyPersistentCacheBackend
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.retryBackend.RetryBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BackendFactory(
    private val eventQueue: AnalyticsEventQueue,
    private val usePersistentEventCacheOnFailedConnections: Boolean = false,
) {
    fun createBackend(config: BitmovinAnalyticsConfig, context: Context): Backend {
        val httpBackend = HttpBackend(config.config, context)
        val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()) // TODO scope?

        return if (usePersistentEventCacheOnFailedConnections) {
            ReadOnlyPersistentCacheBackend(
                coroutineScope,
                PersistentCacheBackend(
                    httpBackend,
                    eventQueue,
                ),
                eventQueue,
            )
        } else {
            val backend = ReadOnlyPersistentCacheBackend(
                coroutineScope,
                httpBackend,
                eventQueue,
            )
            if (config.config.tryResendDataOnFailedConnection) {
                RetryBackend(backend, Handler())
            } else {
                backend
            }
        }
    }
}
