package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.google.android.exoplayer2.ExoPlayer

class FakeExoPlayerAdapter(
    exoplayer: ExoPlayer,
    config: BitmovinAnalyticsConfig,
    deviceInformationProvider: DeviceInformationProvider,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory
) : ExoPlayerAdapter(exoplayer, config, deviceInformationProvider, stateMachine, featureFactory) {

    var fakePosition: Long = 0

    override fun registerEventDataManipulators(pipeline: EventDataManipulatorPipeline) {
    }

    override fun getPosition(): Long {
        return this.fakePosition
    }
}
