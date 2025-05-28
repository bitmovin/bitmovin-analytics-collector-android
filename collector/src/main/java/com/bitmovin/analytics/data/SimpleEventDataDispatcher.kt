package com.bitmovin.analytics.data

import android.content.Context
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.license.AuthenticationCallback
import com.bitmovin.analytics.license.AuthenticationResponse
import com.bitmovin.analytics.license.LicenseCall
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.license.LicensingState
import com.bitmovin.analytics.utils.ScopeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

internal class SimpleEventDataDispatcher(
    private val context: Context,
    private val config: AnalyticsConfig,
    private val callback: LicenseCallback?,
    private val backendFactory: BackendFactory,
    private val licenseCall: LicenseCall,
    private val scopeProvider: ScopeProvider,
) : IEventDataDispatcher, AuthenticationCallback {
    private lateinit var backend: Backend
    private lateinit var ioScope: CoroutineScope
    private val data: Queue<EventData> = ConcurrentLinkedQueue()
    private val adData: Queue<AdEventData> = ConcurrentLinkedQueue()
    private var enabled = false

    init {
        createBackend()
    }

    private fun createBackend() {
        ioScope = scopeProvider.createIoScope()
        backend = backendFactory.createBackend(config, context, ioScope)
    }

    @Synchronized
    override fun authenticationCompleted(response: AuthenticationResponse) {
        val success =
            when (response) {
                is AuthenticationResponse.Granted -> {
                    callback?.configureFeatures(
                        LicensingState.Authenticated(response.licenseKey),
                        response.featureConfigContainer,
                    )
                    enabled = true
                    forwardQueuedEvents(response.licenseKey)
                    true
                }

                is AuthenticationResponse.Denied, AuthenticationResponse.Error -> {
                    callback?.configureFeatures(LicensingState.Unauthenticated, null)
                    false
                }
            }
        callback?.authenticationCompleted(success)
    }

    override fun enable() {
        createBackend()
        ioScope.launch { licenseCall.authenticate(this@SimpleEventDataDispatcher) }
    }

    override fun disable() {
        data.clear()
        adData.clear()
        ioScope.cancel()
        enabled = false
    }

    override fun add(eventData: EventData) {
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

    private fun forwardQueuedEvents(licenseKey: String) {
        val it = data.iterator()
        while (it.hasNext()) {
            val eventData = it.next()
            backend.send(
                if (eventData.key == null) eventData.copy(key = licenseKey) else eventData,
            )
            it.remove()
        }
        val adIt = adData.iterator()
        while (adIt.hasNext()) {
            val eventData = adIt.next()
            backend.sendAd(
                if (eventData.key == null) eventData.copy(key = licenseKey) else eventData,
            )
            adIt.remove()
        }
    }
}
