package com.bitmovin.analytics

import android.content.Context
import android.util.Log
import com.bitmovin.analytics.BitmovinAnalyticsConfigExtension.Companion.getCustomData
import com.bitmovin.analytics.BitmovinAnalyticsConfigExtension.Companion.setCustomData
import com.bitmovin.analytics.SourceMetadataExtension.Companion.getCustomData
import com.bitmovin.analytics.SourceMetadataExtension.Companion.setCustomData
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.data.DebuggingEventDataDispatcher
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.SimpleEventDataDispatcher
import com.bitmovin.analytics.features.FeatureManager
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.stateMachines.DefaultStateMachineListener
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.stateMachines.StateMachineListener

/**
 * An analytics plugin that sends video playback analytics to Bitmovin Analytics servers. Currently
 * supports analytics of ExoPlayer video players
 */
class BitmovinAnalytics
    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
     * @param context [Context]
     */
    (val config: BitmovinAnalyticsConfig, val context: Context) : LicenseCallback {
    private val debugCallback: DebugCallback = object : DebugCallback {
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
    private val eventBus = EventBus()
    private val eventDataDispatcher = DebuggingEventDataDispatcher(SimpleEventDataDispatcher(config, this.context, this, BackendFactory()), debugCallback)

    private var playerAdapter: PlayerAdapter? = null
    private var stateMachineListener: StateMachineListener? = null
    private val adAnalytics: BitmovinAdAnalytics? = if (config.ads) BitmovinAdAnalytics(this) else null

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
        val stateMachineListener = DefaultStateMachineListener(this, adapter, eventBus[OnErrorDetailEventListener::class])
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
        // TODO reset features and prepare for new source
        playerAdapter?.resetSourceRelatedState()
    }

    var customData: CustomData
        get() {
            val sourceMetadata = playerAdapter?.currentSourceMetadata
            return sourceMetadata?.getCustomData() ?: config.getCustomData()
        }
        set(customData) {
            var setCustomDataFunction: (CustomData) -> Unit = { config.setCustomData(it) }
            val sourceMetadata = playerAdapter?.currentSourceMetadata
            if (sourceMetadata != null) {
                setCustomDataFunction = { sourceMetadata.setCustomData(it) }
            }
            playerAdapter?.stateMachine?.changeCustomData(position, customData, setCustomDataFunction)
        }

    fun setCustomDataOnce(customData: CustomData) {
        val playerAdapter = this.playerAdapter
        if (playerAdapter == null) {
            Log.d(TAG, "Custom data could not be set because player is not attached")
            return
        }
        var getCustomDataFunction: () -> CustomData = { config.getCustomData() }
        var setCustomDataFunction: (CustomData) -> Unit = { config.setCustomData(it) }
        val sourceMetadata = playerAdapter.currentSourceMetadata
        if (sourceMetadata != null) {
            getCustomDataFunction = { sourceMetadata.getCustomData() }
            setCustomDataFunction = { sourceMetadata.setCustomData(it) }
        }
        val currentCustomData = getCustomDataFunction()
        setCustomDataFunction(customData)
        val eventData = playerAdapter.createEventData()
        eventData.state = PlayerStates.CUSTOMDATACHANGE.name
        sendEventData(eventData)
        setCustomDataFunction(currentCustomData)
    }

    fun sendEventData(data: EventData) {
        eventDataDispatcher.add(data)
        playerAdapter?.clearValues()
    }

    fun sendAdEventData(data: AdEventData) {
        eventDataDispatcher.addAd(data)
    }

    val position: Long
        get() = playerAdapter?.position ?: 0
    val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>
        get() = eventBus[OnAnalyticsReleasingEventListener::class]
    val onErrorDetailObservable: Observable<OnErrorDetailEventListener>
        get() = eventBus[OnErrorDetailEventListener::class]

    override fun configureFeatures(authenticated: Boolean, featureConfigs: FeatureConfigContainer?) {
        featureManager.configureFeatures(authenticated, featureConfigs)
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