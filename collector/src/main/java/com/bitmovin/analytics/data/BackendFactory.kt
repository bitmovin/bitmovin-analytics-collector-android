package com.bitmovin.analytics.data

import android.content.Context
import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.persistence.ConsumeOnlyPersistentCacheBackend
import com.bitmovin.analytics.persistence.PersistentCacheBackend
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.retryBackend.RetryBackend
import kotlinx.coroutines.CoroutineScope

class BackendFactory(
    private val eventQueue: AnalyticsEventQueue,
    private val usePersistentEventCacheOnFailedConnections: Boolean = false,
) {
    fun createBackend(
        config: BitmovinAnalyticsConfig,
        context: Context,
        scope: CoroutineScope,
    ): Backend {
        val httpBackend = HttpBackend(config.config, context)

        return if (usePersistentEventCacheOnFailedConnections) {
            ConsumeOnlyPersistentCacheBackend(
                scope,
                PersistentCacheBackend(
                    httpBackend,
                    eventQueue,
                ),
                eventQueue,
            )
        } else {
            val backend = ConsumeOnlyPersistentCacheBackend(
                scope,
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
