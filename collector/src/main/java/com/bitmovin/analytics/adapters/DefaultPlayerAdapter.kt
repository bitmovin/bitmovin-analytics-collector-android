package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.data.manipulators.ManifestUrlEventDataManipulator
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

abstract class DefaultPlayerAdapter(protected val config: BitmovinAnalyticsConfig, private val eventDataFactory: EventDataFactory, override val stateMachine: PlayerStateMachine, private val featureFactory: FeatureFactory, private val deviceInformationProvider: DeviceInformationProvider) : PlayerAdapter {
    protected abstract val eventDataManipulators: Collection<EventDataManipulator>

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        eventDataManipulators.forEach { eventDataFactory.registerEventDataManipulator(it) }
        eventDataFactory.registerEventDataManipulator(ManifestUrlEventDataManipulator(this, config))
        return featureFactory.createFeatures()
    }

    override fun createEventData() =
        eventDataFactory.create(
            stateMachine.impressionId,
            currentSourceMetadata,
            deviceInformationProvider.getDeviceInformation(),
            playerInfo,
        )

    override fun release() {
        eventDataFactory.clearEventDataManipulators()
        stateMachine.release()
    }
}
