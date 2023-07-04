package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.analytics.AnalyticsListener

internal class BitmovinPlayerListener(
    private val stateMachine: PlayerStateMachine,
    private val playerContext: PlayerContext,
    private val playbackQualityProvider: PlaybackQualityProvider,
) : AnalyticsListener {

// TODO: can we use the AnalyticsListener here? or what is a good way of refactoring the big Adapter class?
}
