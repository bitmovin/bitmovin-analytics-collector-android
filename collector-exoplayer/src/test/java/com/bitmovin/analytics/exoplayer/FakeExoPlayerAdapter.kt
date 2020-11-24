package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.EventDataDecoratorPipeline
import com.bitmovin.analytics.data.DeviceInformationEventDataDecorator
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.ExoPlayer

class FakeExoPlayerAdapter(exoplayer: ExoPlayer, config: BitmovinAnalyticsConfig, deviceInformationDecorator: DeviceInformationEventDataDecorator, stateMachine: PlayerStateMachine) : ExoPlayerAdapter(exoplayer, config, deviceInformationDecorator, stateMachine) {
    var fakePosition: Long = 0

    override fun registerEventDataDecorators(pipeline: EventDataDecoratorPipeline?) {
    }

    override fun getPosition(): Long {
        return this.fakePosition
    }
}
