package com.bitmovin.analytics.data

import android.content.Context
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.RetryPolicy
import com.bitmovin.analytics.data.persistence.EventDatabase
import com.bitmovin.analytics.license.DefaultLicenseCall
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.license.LicenseKeyProvider
import com.bitmovin.analytics.persistence.EventQueueConfig
import com.bitmovin.analytics.persistence.EventQueueFactory
import com.bitmovin.analytics.persistence.PersistingAuthenticatedDispatcher
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.utils.ScopeProvider

class EventDataDispatcherFactory(
    private val context: Context,
    private val config: AnalyticsConfig,
    licenseKeyProvider: LicenseKeyProvider,
    private val eventQueue: AnalyticsEventQueue =
        EventQueueFactory.createPersistentEventQueue(
            EventQueueConfig(),
            EventDatabase.getInstance(context),
        ),
) {
    private val scopeProvider = ScopeProvider.create()
    private val licenseCall = DefaultLicenseCall(config, licenseKeyProvider, context)
    private val backendFactory = BackendFactory(eventQueue)

    fun create(callback: LicenseCallback): IEventDataDispatcher {
        return if (config.retryPolicy == RetryPolicy.LONG_TERM) {
            PersistingAuthenticatedDispatcher(
                context = context,
                config = config,
                callback = callback,
                backendFactory = backendFactory,
                licenseCall = licenseCall,
                eventQueue = eventQueue,
                scopeProvider = scopeProvider,
            )
        } else {
            SimpleEventDataDispatcher(
                context = context,
                config = config,
                callback = callback,
                backendFactory = backendFactory,
                licenseCall = licenseCall,
                scopeProvider = scopeProvider,
            )
        }
    }
}
