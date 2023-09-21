package com.bitmovin.analytics.data

import android.content.Context
import android.os.Handler
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.RetryPolicy
import com.bitmovin.analytics.persistence.ConsumeOnlyPersistentCacheBackend
import com.bitmovin.analytics.persistence.PersistentCacheBackend
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.retryBackend.RetryBackend
import kotlinx.coroutines.CoroutineScope

class BackendFactory(
    private val eventQueue: AnalyticsEventQueue,
) {
    fun createBackend(
        config: AnalyticsConfig,
        context: Context,
        scope: CoroutineScope,
    ): Backend {
        val innerBackend = HttpBackend(config, context).let {
            if (config.retryPolicy == RetryPolicy.LONG_TERM) {
                PersistentCacheBackend(it, eventQueue)
            } else {
                it
            }
        }

        val backend = ConsumeOnlyPersistentCacheBackend(
            scope,
            innerBackend,
            eventQueue,
        )
        // The persistent event cache already tries resending events
        // The RetryBackend and the PersistentCacheBackend may not be mixed,
        // to avoid "fighting" implementations.
        return if (config.retryPolicy == RetryPolicy.SHORT_TERM) {
            // TODO (AN-3404): get rid of the deprecated Handler()
            RetryBackend(backend, Handler())
        } else {
            backend
        }
    }
}
