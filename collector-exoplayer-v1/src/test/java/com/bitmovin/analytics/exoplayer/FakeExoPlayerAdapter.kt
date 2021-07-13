package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.ExoPlayer

class FakeExoPlayerAdapter(
    exoplayer: ExoPlayer,
    config: BitmovinAnalyticsConfig,
    deviceInformationProvider: DeviceInformationProvider,
    stateMachine: PlayerStateMachine
) : ExoPlayerAdapter(exoplayer, config, deviceInformationProvider, stateMachine) {

    var fakePosition: Long = 0

    override fun registerEventDataManipulators(pipeline: EventDataManipulatorPipeline) {
    }

    override fun getPosition(): Long {
        return this.fakePosition
    }
}
