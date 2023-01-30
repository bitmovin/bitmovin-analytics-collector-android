package com.bitmovin.analytics.amazon.ivs

import android.util.Log
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.Quality
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates

class AmazonIvsPlayerAdapter(
    private val player: Player,
    config: BitmovinAnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
) : DefaultPlayerAdapter(config, eventDataFactory, stateMachine, featureFactory, deviceInformationProvider), EventDataManipulator {

    init {
        attachAnalyticsListener()
    }

    private fun attachAnalyticsListener() {
        player.addListener(createAnalyticsListener())
    }

    private fun createAnalyticsListener(): Player.Listener {
        return object : Player.Listener() {
            override fun onCue(p0: Cue) {
//                TODO("Not yet implemented")
            }

            override fun onDurationChanged(p0: Long) {
//                TODO("Not yet implemented")
            }

            override fun onStateChanged(state: Player.State) {
                Log.d(TAG, "onStateChanged state: $state")
                when (state) {
                    Player.State.READY ->
                        stateMachine.transitionState(PlayerStates.READY, position)

                    Player.State.BUFFERING -> {
                        // TODO this condition is just to test setup
                        if (!stateMachine.isStartupFinished) {
                            stateMachine.transitionState(PlayerStates.STARTUP, position)
                        }

                        stateMachine.transitionState(PlayerStates.BUFFERING, position)
                    }
                    Player.State.PLAYING ->
                        stateMachine.transitionState(PlayerStates.PLAYING, position)

                    else -> Log.d(TAG, "No state transition possible for player state")
                }

//                TODO("Not yet implemented")
            }

            override fun onError(p0: PlayerException) {
//                TODO("Not yet implemented")
            }

            override fun onRebuffering() {
//                TODO("Not yet implemented")
            }

            override fun onSeekCompleted(p0: Long) {
//                TODO("Not yet implemented")
            }

            override fun onVideoSizeChanged(p0: Int, p1: Int) {
//                TODO("Not yet implemented")
            }

            override fun onQualityChanged(p0: Quality) {
//                TODO("Not yet implemented")
            }
        }
    }

    override fun manipulate(data: EventData) {
        data.version = player.version
    }

    override val eventDataManipulators: Collection<EventDataManipulator> by lazy { listOf(this) } // TODO("Not yet implemented")

    override val position: Long
        get() = player.position // TODO check if conversion to milliseconds is needed

    override val drmDownloadTime: Long?
        get() = null // TODO("Not yet implemented")
    override val currentSourceMetadata: SourceMetadata?
        get() = null // TODO("Not yet implemented")

    override fun resetSourceRelatedState() {
//        TODO("Not yet implemented")
    }

    override fun clearValues() {
//        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "AmazonIVSPlayerAdapter"
    }
}
