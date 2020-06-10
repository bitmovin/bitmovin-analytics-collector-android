package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.ExoPlayer

class FakeExoPlayerAdapter(exoplayer: ExoPlayer, config: BitmovinAnalyticsConfig, factory: EventDataFactory, stateMachine: PlayerStateMachine) : ExoPlayerAdapter(exoplayer, config, factory, stateMachine) {
    var fakePosition: Long = 0

    override fun getPosition(): Long {
        return this.fakePosition
    }
}