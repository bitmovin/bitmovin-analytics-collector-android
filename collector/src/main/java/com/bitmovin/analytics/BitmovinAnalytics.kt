package com.bitmovin.analytics

import android.content.Context
import android.util.Log
import com.bitmovin.analytics.SourceMetadataExtension.Companion.getCustomData
import com.bitmovin.analytics.SourceMetadataExtension.Companion.setCustomData
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.data.DebuggingEventDataDispatcher
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.RandomizedUserIdIdProvider
import com.bitmovin.analytics.data.SecureSettingsAndroidIdUserIdProvider
import com.bitmovin.analytics.data.SimpleEventDataDispatcher
import com.bitmovin.analytics.data.UserIdProvider
import com.bitmovin.analytics.data.manipulators.ManifestUrlEventDataManipulator
import com.bitmovin.analytics.features.FeatureManager
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.stateMachines.DefaultStateMachineListener
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
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
    (val config: BitmovinAnalyticsConfig, val context: Context) : LicenseCallback, ImpressionIdProvider {
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
    private val userIdProvider: UserIdProvider = if (config.randomizeUserId) RandomizedUserIdIdProvider() else SecureSettingsAndroidIdUserIdProvider(context)
    private var playerAdapter: PlayerAdapter? = null
    private var stateMachineListener: StateMachineListener? = null
    private val adAnalytics: BitmovinAdAnalytics? = if (config.ads) BitmovinAdAnalytics(this) else null

    val playerStateMachine = PlayerStateMachine(config, this)
    val eventDataFactory = EventDataFactory(config, userIdProvider)

    /**
     * Attach a player instance to this analytics plugin. After this is completed, BitmovinAnalytics
     * will start monitoring and sending analytics data based on the attached player adapter.
     *
     *
     * To attach a different player instance, simply call this method again.
     */
    fun attach(adapter: PlayerAdapter) {
        detachPlayer()
        stateMachineListener = DefaultStateMachineListener(this, adapter, eventBus[OnErrorDetailEventListener::class])
        playerStateMachine.addListener(stateMachineListener)
        eventDataDispatcher.enable()
        playerAdapter = adapter
        val features = adapter.init()
        featureManager.registerFeatures(features)

        // this.registerEventDataManipulators(prePipelineManipulator);
        adapter.registerEventDataManipulators(eventDataFactory)
        eventDataFactory.registerEventDataManipulator(ManifestUrlEventDataManipulator(adapter, config))
        // this.registerEventDataManipulators(postPipelineManipulator);
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
        if (stateMachineListener != null) {
            playerStateMachine.removeListener(stateMachineListener)
        }
        playerStateMachine.resetStateMachine()
        eventDataDispatcher.disable()
        eventDataFactory.clearEventDataManipulators()
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
            return sourceMetadata?.getCustomData() ?: config.customData
        }
        set(customData) {
            var customDataSetter: (CustomData) -> Unit = config::setCustomData
            val sourceMetadata = playerAdapter?.currentSourceMetadata
            if (sourceMetadata != null) {
                customDataSetter = { sourceMetadata.setCustomData(it) }
            }
            val toBeRemovedOnceStateMachineIsKotlin = object : CustomDataHelpers.Setter {
                override fun setCustomData(customData: CustomData) {
                    customDataSetter(customData)
                }
            }
            playerStateMachine.changeCustomData(position, customData, toBeRemovedOnceStateMachineIsKotlin)
        }

    fun setCustomDataOnce(customData: CustomData) {
        val playerAdapter = this.playerAdapter
        if (playerAdapter == null) {
            Log.d(TAG, "Custom data could not be set because player is not attached")
            return
        }
        var customDataGetter: () -> CustomData = config::getCustomData
        var customDataSetter: (CustomData) -> Unit = config::setCustomData
        val sourceMetadata = playerAdapter.currentSourceMetadata
        if (sourceMetadata != null) {
            customDataGetter = { sourceMetadata.getCustomData() }
            customDataSetter = { sourceMetadata.setCustomData(it) }
        }
        val currentCustomData = customDataGetter()
        customDataSetter(customData)
        val eventData = playerAdapter.createEventData()
        eventData.state = PlayerStates.CUSTOMDATACHANGE.name
        sendEventData(eventData)
        customDataSetter(currentCustomData)
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

    override val impressionId: String
        get() = playerStateMachine.impressionId

    interface DebugListener {
        fun onDispatchEventData(data: EventData)
        fun onDispatchAdEventData(data: AdEventData)
        fun onMessage(message: String)
    }

    companion object {
        private const val TAG = "BitmovinAnalytics"
    }
}
