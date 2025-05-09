package com.bitmovin.analytics

import android.app.Application
import android.content.Context
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataDispatcherFactory
import com.bitmovin.analytics.data.SEQUENCE_NUMBER_LIMIT
import com.bitmovin.analytics.data.SequenceNumberAndImpressionIdProvider
import com.bitmovin.analytics.features.FeatureManager
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.analytics.internal.InternalBitmovinApi
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.license.InstantLicenseKeyProvider
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.license.LicenseKeyProvider
import com.bitmovin.analytics.license.LicensingState
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.stateMachines.DefaultStateMachineListener
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.stateMachines.StateMachineListener
import com.bitmovin.analytics.utils.ApiV3Utils
import com.bitmovin.analytics.utils.BitmovinLog

@InternalBitmovinApi
class BitmovinAnalytics(
    val config: AnalyticsConfig,
    val context: Context,
    licenseKeyProvider: LicenseKeyProvider = InstantLicenseKeyProvider(config.licenseKey),
    eventDataDispatcherFactory: EventDataDispatcherFactory = EventDataDispatcherFactory(context, config, licenseKeyProvider),
    private val sequenceNumberAndImpressionIdProvider: SequenceNumberAndImpressionIdProvider = SequenceNumberAndImpressionIdProvider(),
) : LicenseCallback {
    private val eventBus = EventBus()
    private var lifecycleCallbacks: ActivityLifecycleCallbacks? = null
    private val featureManager = FeatureManager<FeatureConfigContainer>()
    private val eventDataDispatcher = eventDataDispatcherFactory.create(this)
    private var stateMachineListener: StateMachineListener? = null
    private val adAnalytics: BitmovinAdAnalytics? =
        if (!config.adTrackingDisabled) BitmovinAdAnalytics(this) else null

    // Setting a playerStartupTime of 1 to workaround dashboard issue (only for the
    // first startup sample, in case the collector supports multiple sources)
    private var playerStartupTime = 1L

    public var playerAdapter: PlayerAdapter? = null

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
        val stateMachineListener =
            DefaultStateMachineListener(this, adapter, eventBus[OnErrorDetailEventListener::class], adapter.ssaiService)
        adapter.stateMachine.subscribe(stateMachineListener)
        this.stateMachineListener = stateMachineListener
        eventDataDispatcher.enable()
        playerAdapter = adapter
        val features = adapter.init()
        featureManager.registerFeatures(features)
        registerActivityPauseListener(adapter.ssaiService)
        tryAttachAd(adapter)
    }

    private fun tryAttachAd(adapter: PlayerAdapter) {
        val adAnalytics = this.adAnalytics ?: return
        val adAdapter = adapter.createAdAdapter() ?: return
        adAnalytics.attachAdapter(adapter, adAdapter)
    }

    /** Detach the current player that is being used with Bitmovin Analytics.  */
    fun detachPlayer(shouldSendOutSamples: Boolean = true) {
        if (shouldSendOutSamples) {
            playerAdapter?.ssaiService?.flushCurrentAdSample()
            playerAdapter?.triggerLastSampleOfSession()
        }
        adAnalytics?.detachAdapter()
        featureManager.unregisterFeatures()
        eventBus.notify(OnAnalyticsReleasingEventListener::class) { it.onReleasing() }
        playerAdapter?.release()
        eventDataDispatcher.disable()
        unregisterActivityPauseListener()
    }

    fun resetSourceRelatedState() {
        featureManager.resetFeatures()
        playerAdapter?.resetSourceRelatedState()
        sequenceNumberAndImpressionIdProvider.reset()
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
            BitmovinLog.d(TAG, "Custom data event could not be sent because player is not attached")
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
        data.sequenceNumber = sequenceNumberAndImpressionIdProvider.getAndIncrementSequenceNumber()

        // Do not send events with sequence number greater than the limit, the limit is in sync with the limit on ingress side
        if (data.sequenceNumber > SEQUENCE_NUMBER_LIMIT) {
            BitmovinLog.d("BitmovinAnalytics", "Event with sequence number ${data.sequenceNumber} is not sent.")
            return
        }

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
            detachPlayer(shouldSendOutSamples = false)
        }
    }

    val impressionId: String
        get() = sequenceNumberAndImpressionIdProvider.getImpressionId()

    companion object {
        private const val TAG = "BitmovinAnalytics"
    }

    private fun registerActivityPauseListener(ssaiService: SsaiService) {
        val lifecycleCallbacks = ActivityLifecycleCallbacks(ssaiService)
        try {
            val application = this.context.applicationContext as Application
            application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
            this.lifecycleCallbacks = lifecycleCallbacks
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while registering lifecycle callbacks", e)
        }
    }

    private fun unregisterActivityPauseListener() {
        val lifecycleCallbacks = this.lifecycleCallbacks
        try {
            if (lifecycleCallbacks != null) {
                val application = this.context.applicationContext as Application
                application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
                this.lifecycleCallbacks = null
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while unregistering lifecycle callbacks", e)
        }
    }
}
