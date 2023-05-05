package com.bitmovin.analytics.data

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.license.AuthenticationCallback
import com.bitmovin.analytics.license.AuthenticationResponse
import com.bitmovin.analytics.license.DefaultLicenseCall
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.utils.ScopeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

internal class SimpleEventDataDispatcher(
    private val context: Context,
    private val config: BitmovinAnalyticsConfig,
    private val callback: LicenseCallback?,
    private val backendFactory: BackendFactory,
    private val scopeProvider: ScopeProvider,
) : IEventDataDispatcher, AuthenticationCallback {
    private lateinit var backend: Backend
    private lateinit var scope: CoroutineScope
    private val data: Queue<EventData>
    private val adData: Queue<AdEventData>
    private var enabled = false

    private var sampleSequenceNumber = 0

    init {
        data = ConcurrentLinkedQueue()
        adData = ConcurrentLinkedQueue()
        createBackend()
    }

    private fun createBackend() {
        scope = scopeProvider.createMainScope()
        backend = backendFactory.createBackend(config, context, scope)
    }

    @Synchronized
    override fun authenticationCompleted(
        response: AuthenticationResponse,
    ) {
        val success = when (response) {
            is AuthenticationResponse.Granted -> {
                callback?.configureFeatures(
                    true,
                    response.featureConfigContainer,
                )
                enabled = true
                forwardQueuedEvents()
                true
            }

            is AuthenticationResponse.Denied, AuthenticationResponse.Error -> {
                callback?.configureFeatures(false, null)
                false
            }
        }
        callback?.authenticationCompleted(success)
    }

    override fun enable() {
        createBackend()
        val licenseCall = DefaultLicenseCall(config, context)
        licenseCall.authenticate(this)
    }

    override fun disable() {
        data.clear()
        adData.clear()
        scope.cancel()
        enabled = false
        sampleSequenceNumber = 0
    }

    override fun add(eventData: EventData) {
        eventData.sequenceNumber = sampleSequenceNumber++
        if (enabled) {
            backend.send(eventData)
        } else {
            data.add(eventData)
        }
    }

    override fun addAd(eventData: AdEventData) {
        if (enabled) {
            backend.sendAd(eventData)
        } else {
            adData.add(eventData)
        }
    }

    override fun resetSourceRelatedState() {
        sampleSequenceNumber = 0
    }

    private fun forwardQueuedEvents() {
        val it = data.iterator()
        while (it.hasNext()) {
            val eventData = it.next()
            backend.send(eventData)
            it.remove()
        }
        val adIt = adData.iterator()
        while (adIt.hasNext()) {
            val eventData = adIt.next()
            backend.sendAd(eventData)
            adIt.remove()
        }
    }
}
