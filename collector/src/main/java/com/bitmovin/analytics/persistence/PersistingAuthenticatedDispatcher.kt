package com.bitmovin.analytics.persistence

import android.content.Context
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.data.CacheConsumingBackend
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.IEventDataDispatcher
import com.bitmovin.analytics.data.SEQUENCE_NUMBER_LIMIT
import com.bitmovin.analytics.license.AuthenticationCallback
import com.bitmovin.analytics.license.AuthenticationResponse
import com.bitmovin.analytics.license.LicenseCall
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.license.LicensingState
import com.bitmovin.analytics.persistence.OperationMode.Authenticated
import com.bitmovin.analytics.persistence.OperationMode.Disabled
import com.bitmovin.analytics.persistence.OperationMode.Unauthenticated
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.utils.ScopeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal class PersistingAuthenticatedDispatcher(
    private val context: Context,
    private val config: AnalyticsConfig,
    callback: LicenseCallback?,
    private val backendFactory: BackendFactory,
    private val licenseCall: LicenseCall,
    private val eventQueue: AnalyticsEventQueue,
    private val scopeProvider: ScopeProvider,
) : IEventDataDispatcher {
    private lateinit var ioScope: CoroutineScope
    private lateinit var backend: Backend
    private var operationMode = Unauthenticated
    private var sampleSequenceNumber = 0

    init {
        createBackend()
    }

    private fun createBackend() {
        ioScope = scopeProvider.createIoScope()
        backend = backendFactory.createBackend(config, context, ioScope)
    }

    private val authenticationCallback =
        AuthenticationCallback { response ->
            val success =
                when (response) {
                    is AuthenticationResponse.Granted -> {
                        callback?.configureFeatures(
                            LicensingState.Authenticated(response.licenseKey),
                            featureConfigs = response.featureConfigContainer,
                        )
                        if (operationMode != Authenticated) {
                            (backend as? CacheConsumingBackend)?.startCacheFlushing()
                        }
                        operationMode = Authenticated
                        true
                    }

                    is AuthenticationResponse.Denied -> {
                        callback?.configureFeatures(
                            LicensingState.Unauthenticated,
                            featureConfigs = null,
                        )
                        disable()
                        eventQueue.clear()
                        false
                    }

                    is AuthenticationResponse.Error -> {
                        return@AuthenticationCallback
                    }
                }
            callback?.authenticationCompleted(success)
        }

    override fun enable() {
        operationMode = Unauthenticated
        createBackend()
    }

    override fun disable() {
        ioScope.cancel()
        operationMode = Disabled
        sampleSequenceNumber = 0
    }

    override fun add(data: EventData) {
        // Do not send events with sequence number greater than the limit
        if (sampleSequenceNumber > SEQUENCE_NUMBER_LIMIT) {
            return
        }

        data.sequenceNumber = sampleSequenceNumber++

        when (operationMode) {
            Disabled -> return
            Authenticated -> backend.send(data)
            Unauthenticated -> {
                eventQueue.push(data)
                ioScope.launch { licenseCall.authenticate(authenticationCallback) }
            }
        }
    }

    override fun addAd(data: AdEventData) {
        when (operationMode) {
            Disabled -> return
            Authenticated -> backend.sendAd(data)
            Unauthenticated -> {
                eventQueue.push(data)
                ioScope.launch { licenseCall.authenticate(authenticationCallback) }
            }
        }
    }

    override fun resetSourceRelatedState() {
        sampleSequenceNumber = 0
    }
}

private enum class OperationMode {
    Authenticated,
    Unauthenticated,
    Disabled,
}
