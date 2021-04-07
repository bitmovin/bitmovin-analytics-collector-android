package com.bitmovin.analytics.bitmovin.player

import android.util.Log
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.bitmovin.player.config.BitmovinAnalyticsSourceConfigProvider
import com.bitmovin.analytics.config.AnalyticsSourceConfig
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent

internal class SourceSwitchHandler(
        val adapter: BitmovinSdkAdapter,
        val config: BitmovinAnalyticsConfig,
        val sourceConfigProvider: BitmovinAnalyticsSourceConfigProvider,
        val stateMachine: PlayerStateMachine,
        val bitmovinPlayer: Player) {

    enum class SourceSwitchState {
        Playing,
        StartupBuffering,
        Startup
    }

    private val TAG = "SourceSwitchHandler"
    private var state = SourceSwitchState.Playing


    fun init() {
        val playerSource = bitmovinPlayer.source ?: return
        // if collector is attached to player after the player has loaded data
        val sourceConfig = sourceConfigProvider.getSource(playerSource)
        if (sourceConfig != null) {
            updateConfig(sourceConfig)
        }
    }

    private fun sourceEventSourceLoadedListener(event: SourceEvent.Loaded) {
        Log.d(TAG, "On Source Loaded: ${event.source.config.url}")
        Log.d(TAG, "current source: ${bitmovinPlayer.source?.config?.url}")
    }

    private fun playerEventPlaylistTransitionListener(event: PlayerEvent.PlaylistTransition) {
        try {
            Log.d(TAG, "Event PlaylistTransition: from: ${event.from.config.url} to: ${event.to.config.url}")
            val sourceConfig = sourceConfigProvider.getSource(event.to)
            stateMachine.sourceChange(sourceConfig, event.timestamp)
            stateMachine.transitionState(PlayerState.STARTUP, adapter.position)
            state = SourceSwitchState.Startup
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun playerEventStallStartedListener(event: PlayerEvent.StallStarted) {
        if (state == SourceSwitchState.Startup) {
            state = SourceSwitchState.StartupBuffering
        }
    }

    private fun playerEventStallEndedListener(event: PlayerEvent.StallEnded) {
        if (state == SourceSwitchState.StartupBuffering) {
            state = SourceSwitchState.Playing
            // statemachine transition to PLAYING will be handled by adapter
        }
    }

    private fun playerEventTimeChangedListener(event: PlayerEvent.TimeChanged) {
        if (state == SourceSwitchState.Startup) {
            stateMachine.transitionState(PlayerState.PLAYING, adapter.position)
            state = SourceSwitchState.Playing
        }
    }

    fun addPlayerListener() {
        bitmovinPlayer.on(PlayerEvent.PlaylistTransition::class, this::playerEventPlaylistTransitionListener)
        bitmovinPlayer.on(PlayerEvent.TimeChanged::class, this::playerEventTimeChangedListener)
        bitmovinPlayer.on(PlayerEvent.StallStarted::class, this::playerEventStallStartedListener)
        bitmovinPlayer.on(PlayerEvent.StallEnded::class, this::playerEventStallEndedListener)
    }

    fun removePlayerListener() {
        bitmovinPlayer.off(this::playerEventPlaylistTransitionListener)
        bitmovinPlayer.off(this::playerEventTimeChangedListener)
        bitmovinPlayer.off(this::playerEventStallStartedListener)
        bitmovinPlayer.off(this::playerEventStallEndedListener)
    }

    private fun updateConfig(sourceConfig: AnalyticsSourceConfig) {
        this.config.cdnProvider = sourceConfig.cdnProvider
        this.config.customData1 = sourceConfig.customData1
        this.config.customData2 = sourceConfig.customData2
        this.config.customData3 = sourceConfig.customData3
        this.config.customData4 = sourceConfig.customData4
        this.config.customData5 = sourceConfig.customData5
        this.config.customData6 = sourceConfig.customData6
        this.config.customData7 = sourceConfig.customData7
        this.config.experimentName = sourceConfig.experimentName
        this.config.m3u8Url = sourceConfig.m3u8Url
        this.config.mpdUrl = sourceConfig.mpdUrl
        this.config.path = sourceConfig.path
        this.config.title = sourceConfig.title
        this.config.videoId = sourceConfig.videoId
        this.config.setIsLive(sourceConfig.isLive)
    }
}