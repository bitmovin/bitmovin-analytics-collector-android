package com.bitmovin.analytics

import android.content.Context
import android.util.Log
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.RetryPolicy
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.data.DebuggingEventDataDispatcher
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.SimpleEventDataDispatcher
import com.bitmovin.analytics.data.persistence.EventDatabase
import com.bitmovin.analytics.features.FeatureManager
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.analytics.internal.InternalBitmovinApi
import com.bitmovin.analytics.license.DefaultLicenseCall
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.license.InstantLicenseKeyProvider
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.license.LicenseKeyProvider
import com.bitmovin.analytics.license.LicensingState
import com.bitmovin.analytics.persistence.EventQueueConfig
import com.bitmovin.analytics.persistence.EventQueueFactory
import com.bitmovin.analytics.persistence.PersistingAuthenticatedDispatcher
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.stateMachines.DefaultStateMachineListener
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.stateMachines.StateMachineListener
import com.bitmovin.analytics.utils.ApiV3Utils
import com.bitmovin.analytics.utils.ScopeProvider

@InternalBitmovinApi
class BitmovinAnalytics(
    val config: AnalyticsConfig,
    val context: Context,
    val eventQueue: AnalyticsEventQueue =
        EventQueueFactory.createPersistentEventQueue(
            EventQueueConfig(),
            EventDatabase.getInstance(context),
        ),
    licenseKeyProvider: LicenseKeyProvider = InstantLicenseKeyProvider(config.licenseKey),
) : LicenseCallback {
    private val licenseCall = DefaultLicenseCall(config, licenseKeyProvider, context)
    private val scopeProvider = ScopeProvider.create()
    private val backendFactory = BackendFactory(eventQueue)
    private val eventBus = EventBus()

    private val debugCallback: DebugCallback =
        object : DebugCallback {
            override fun dispatchEventData(data: EventData) {
                eventBus.notify(DebugListener::class) { it.onDispatchEventData(data) }
            }

            override fun dispatchAdEventData(data: AdEventData) {
                eventBus.notify(DebugListener::class) { it.onDispatchAdEventData(data) }
            }

            override fun message(message: String) {
                eventBus.notify(DebugListener::class) { it.onMessage(message) }
            }
        }
    private val featureManager = FeatureManager<FeatureConfigContainer>()

    private val eventDataDispatcher =
        DebuggingEventDataDispatcher(
            if (config.retryPolicy == RetryPolicy.LONG_TERM) {
                PersistingAuthenticatedDispatcher(
                    context = context,
                    config = config,
                    callback = this,
                    backendFactory = backendFactory,
                    licenseCall = licenseCall,
                    eventQueue = eventQueue,
                    scopeProvider = scopeProvider,
                )
            } else {
                SimpleEventDataDispatcher(
                    context = context,
                    config = config,
                    callback = this,
                    backendFactory = backendFactory,
                    licenseCall = licenseCall,
                    scopeProvider = scopeProvider,
                )
            },
            debugCallback,
        )

    private var playerAdapter: PlayerAdapter? = null
    private var stateMachineListener: StateMachineListener? = null
    private val adAnalytics: BitmovinAdAnalytics? =
        if (!config.adTrackingDisabled) BitmovinAdAnalytics(this) else null

    // Setting a playerStartupTime of 1 to workaround dashboard issue (only for the
    // first startup sample, in case the collector supports multiple sources)
    private var playerStartupTime = 1L

    fun getAndResetPlayerStartupTime(): Long {
        val playerStartupTime = playerStartupTime
        this.playerStartupTime = 0
        return playerStartupTime
    }

    /**
     * Attach a player instance to this analytics plugin. After this is completed, BitmovinAnalytics
     * will start monitoring and sending analytics data based on the attached player adapter.
     *
     *
     * To attach a different player instance, simply call this method again.
     */
    fun attach(adapter: PlayerAdapter) {
        detachPlayer()
        val stateMachineListener =
            DefaultStateMachineListener(this, adapter, eventBus[OnErrorDetailEventListener::class])
        adapter.stateMachine.subscribe(stateMachineListener)
        this.stateMachineListener = stateMachineListener
        eventDataDispatcher.enable()
        playerAdapter = adapter
        val features = adapter.init()
        featureManager.registerFeatures(features)
        tryAttachAd(adapter)
    }

    private fun tryAttachAd(adapter: PlayerAdapter) {
        val adAnalytics = this.adAnalytics ?: return
        val adAdapter = adapter.createAdAdapter() ?: return
        adAnalytics.attachAdapter(adapter, adAdapter)
    }

    /** Detach the current player that is being used with Bitmovin Analytics.  */
    fun detachPlayer() {
        detachAd()
        featureManager.unregisterFeatures()
        eventBus.notify(OnAnalyticsReleasingEventListener::class) { it.onReleasing() }
        playerAdapter?.release()
        eventDataDispatcher.disable()
    }

    private fun detachAd() {
        adAnalytics?.detachAdapter()
    }

    fun resetSourceRelatedState() {
        eventDataDispatcher.resetSourceRelatedState()
        featureManager.resetFeatures()
        playerAdapter?.resetSourceRelatedState()
    }

    val activeCustomData: CustomData
        get() {
            val sourceMetadata = playerAdapter?.getCurrentSourceMetadata()
            val defaultMetadata = playerAdapter?.defaultMetadata
            return ApiV3Utils.mergeCustomData(sourceMetadata?.customData, defaultMetadata?.customData)
        }

    fun closeCurrentSampleForCustomDataChangeIfNeeded() {
        playerAdapter?.stateMachine?.closeCurrentSampleForCustomDataChangeIfNeeded(playerAdapter?.position ?: 0)
    }

    fun sendCustomDataEvent(customData: CustomData) {
        val playerAdapter = this.playerAdapter
        if (playerAdapter == null) {
            Log.d(TAG, "Custom data event could not be sent because player is not attached")
            return
        }

        val mergedSourceMetadata = ApiV3Utils.mergeSourceMetadata(playerAdapter.getCurrentSourceMetadata(), playerAdapter.defaultMetadata)
        val mergedCustomData = ApiV3Utils.mergeCustomData(customData, mergedSourceMetadata.customData)
        val activeSourceMetadata = mergedSourceMetadata.copy(customData = mergedCustomData)
        val eventData = playerAdapter.createEventDataForCustomDataEvent(activeSourceMetadata)
        eventData.state = PlayerStates.CUSTOMDATACHANGE.name
        eventData.videoTimeStart = playerAdapter.position
        eventData.videoTimeEnd = eventData.videoTimeStart
        sendEventData(eventData)
    }

    fun sendEventData(data: EventData) {
        eventDataDispatcher.add(data)
    }

    fun sendAdEventData(data: AdEventData) {
        eventDataDispatcher.addAd(data)
    }

    val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>
        get() = eventBus[OnAnalyticsReleasingEventListener::class]
    val onErrorDetailObservable: Observable<OnErrorDetailEventListener>
        get() = eventBus[OnErrorDetailEventListener::class]

    override fun configureFeatures(
        state: LicensingState,
        featureConfigs: FeatureConfigContainer?,
    ) {
        featureManager.configureFeatures(state, featureConfigs)
    }

    override fun authenticationCompleted(success: Boolean) {
        if (!success) {
            detachPlayer()
        }
    }

    fun addDebugListener(listener: DebugListener) {
        eventBus[DebugListener::class].subscribe(listener)
    }

    fun removeDebugListener(listener: DebugListener) {
        eventBus[DebugListener::class].unsubscribe(listener)
    }

    val impressionId: String?
        get() = playerAdapter?.stateMachine?.impressionId

    interface DebugListener {
        fun onDispatchEventData(data: EventData)

        fun onDispatchAdEventData(data: AdEventData)

        fun onMessage(message: String)
    }

    companion object {
        private const val TAG = "BitmovinAnalytics"
    }
}
