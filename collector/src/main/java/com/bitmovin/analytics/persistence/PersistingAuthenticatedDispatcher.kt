package com.bitmovin.analytics.persistence

import android.content.Context
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.data.CacheConsumingBackend
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.IEventDataDispatcher
import com.bitmovin.analytics.license.AuthenticationCallback
import com.bitmovin.analytics.license.AuthenticationResponse
import com.bitmovin.analytics.license.LicenseCall
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.persistence.OperationMode.Authenticated
import com.bitmovin.analytics.persistence.OperationMode.Disabled
import com.bitmovin.analytics.persistence.OperationMode.Unauthenticated
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.utils.ScopeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

internal class PersistingAuthenticatedDispatcher(
    private val context: Context,
    private val config: AnalyticsConfig,
    callback: LicenseCallback?,
    private val backendFactory: BackendFactory,
    private val licenseCall: LicenseCall,
    private val eventQueue: AnalyticsEventQueue,
    private val scopeProvider: ScopeProvider,
) : IEventDataDispatcher {
    private lateinit var scope: CoroutineScope
    private lateinit var backend: Backend
    private var operationMode = Unauthenticated
    private var sampleSequenceNumber = 0

    init {
        createBackend()
    }

    private fun createBackend() {
        scope = scopeProvider.createMainScope()
        backend = backendFactory.createBackend(config, context, scope)
    }

    private val authenticationCallback = AuthenticationCallback { response ->
        val success = when (response) {
            is AuthenticationResponse.Granted -> {
                callback?.configureFeatures(
                    authenticated = true,
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
                    authenticated = false,
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
        scope.cancel()
        operationMode = Disabled
        sampleSequenceNumber = 0
    }

    override fun add(data: EventData) {
        data.sequenceNumber = sampleSequenceNumber++

        when (operationMode) {
            Disabled -> return
            Authenticated -> backend.send(data)
            Unauthenticated -> {
                eventQueue.push(data)
                licenseCall.authenticate(config.licenseKey, authenticationCallback)
            }
        }
    }

    override fun addAd(data: AdEventData) {
        when (operationMode) {
            Disabled -> return
            Authenticated -> backend.sendAd(data)
            Unauthenticated -> {
                eventQueue.push(data)
                licenseCall.authenticate(config.licenseKey, authenticationCallback)
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
