package com.bitmovin.analytics.bitmovin.player

import android.util.Log
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.bitmovin.player.config.BitmovinAnalyticsSourceConfigProvider
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent

internal class SourceSwitchHandler(
    val config: BitmovinAnalyticsConfig,
    val sourceConfigProvider: BitmovinAnalyticsSourceConfigProvider,
    val stateMachine: PlayerStateMachine,
    val bitmovinPlayer: Player
) {

    private val TAG = "SourceSwitchHandler"

    fun init() {
        addPlayerListener()
        updateConfigIfSourceIsAvailable()
    }

    fun destroy() {
        removePlayerListener()
    }

    private fun addPlayerListener() {
        bitmovinPlayer.on(PlayerEvent.PlaylistTransition::class, this::playerEventPlaylistTransitionListener)
        bitmovinPlayer.on(SourceEvent.Loaded::class, this::sourceEventSourceLoadedListener)
    }

    private fun removePlayerListener() {
        bitmovinPlayer.off(this::playerEventPlaylistTransitionListener)
        bitmovinPlayer.off(this::sourceEventSourceLoadedListener)
    }

    private fun updateConfigIfSourceIsAvailable() {
        val playerSource = bitmovinPlayer.source ?: return
        // if collector is attached to player after the player has loaded data and sourceLoaded event already triggered
        val sourceConfig = sourceConfigProvider.getSource(playerSource) ?: return
        config.updateConfig(sourceConfig)
    }

    // Event Handlers

    private fun sourceEventSourceLoadedListener(event: SourceEvent.Loaded) {
        try {
            val sourceConfig = sourceConfigProvider.getSource(event.source) ?: return
            config.updateConfig(sourceConfig)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    private fun playerEventPlaylistTransitionListener(event: PlayerEvent.PlaylistTransition) {
        try {
            Log.d(TAG, "Event PlaylistTransition: from: ${event.from.config.url} to: ${event.to.config.url}")
            val sourceConfig = sourceConfigProvider.getSource(event.to)
            stateMachine.sourceChange(sourceConfig, event.timestamp)
            val positionFromPlayer = BitmovinUtil.getPositionFromPlayer(bitmovinPlayer)
            stateMachine.transitionState(PlayerState.STARTUP, positionFromPlayer)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }
}
