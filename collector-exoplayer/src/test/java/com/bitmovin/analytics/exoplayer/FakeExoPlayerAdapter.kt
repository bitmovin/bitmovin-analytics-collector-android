package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline
import com.bitmovin.analytics.exoplayer.manipulators.BitrateEventDataManipulator
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.ExoPlayer

class FakeExoPlayerAdapter(
    exoplayer: ExoPlayer,
    config: BitmovinAnalyticsConfig,
    deviceInformationProvider: DeviceInformationProvider,
    stateMachine: PlayerStateMachine,
    bitrateEventDataManipulator: BitrateEventDataManipulator
) : ExoPlayerAdapter(exoplayer, config, deviceInformationProvider, stateMachine, bitrateEventDataManipulator) {

    constructor(exoplayer: ExoPlayer,
                config: BitmovinAnalyticsConfig,
                deviceInformationProvider: DeviceInformationProvider,
                stateMachine: PlayerStateMachine) : this(exoplayer, config, deviceInformationProvider, stateMachine, BitrateEventDataManipulator(exoplayer))
    var fakePosition: Long = 0

    override fun registerEventDataManipulators(pipeline: EventDataManipulatorPipeline) {
    }

    override fun getPosition(): Long {
        return this.fakePosition
    }
}
