package com.bitmovin.analytics.persistence

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.IEventDataDispatcher
import com.bitmovin.analytics.license.AuthenticationCallback
import com.bitmovin.analytics.license.AuthenticationResponse
import com.bitmovin.analytics.license.ILicenseCall
import com.bitmovin.analytics.license.LicenseCall
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.persistence.OperationMode.Authenticated
import com.bitmovin.analytics.persistence.OperationMode.Disabled
import com.bitmovin.analytics.persistence.OperationMode.Unauthenticated
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue

internal class OfflineAuthenticatedDispatcher(
    config: BitmovinAnalyticsConfig,
    context: Context,
    callback: LicenseCallback?,
    backendFactory: BackendFactory,
    private val licenseCall: ILicenseCall,
    private val eventQueue: AnalyticsEventQueue,
) : IEventDataDispatcher {
    private val backend: Backend
    private val config: BitmovinAnalyticsConfig
    private val callback: LicenseCallback?
    private val context: Context
    private var operationMode = Unauthenticated
    private var sampleSequenceNumber = 0

    init {
        this.config = config
        this.callback = callback
        this.context = context
        backend = backendFactory.createBackend(config, context)
    }

    private val authenticationCallback = AuthenticationCallback { response ->
        val success = when (response) {
            is AuthenticationResponse.Granted -> {
                callback?.configureFeatures(
                    true,
                    response.featureConfigContainer,
                )
                operationMode = Authenticated
                true
            }

            is AuthenticationResponse.Denied -> {
                callback?.configureFeatures(
                    false,
                    null,
                )
                operationMode = Disabled
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
    }

    override fun disable() {
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
                licenseCall.authenticate(authenticationCallback)
            }
        }
    }

    override fun addAd(data: AdEventData) {
        when (operationMode) {
            Authenticated -> backend.sendAd(data)
            Unauthenticated -> {
                eventQueue.push(data)
                licenseCall.authenticate(authenticationCallback)
            }

            Disabled -> return
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
