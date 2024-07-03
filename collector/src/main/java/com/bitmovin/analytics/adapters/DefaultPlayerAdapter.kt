package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

abstract class DefaultPlayerAdapter(
    protected val config: AnalyticsConfig,
    protected val eventDataFactory: EventDataFactory,
    override val stateMachine: PlayerStateMachine,
    private val featureFactory: FeatureFactory,
    private val deviceInformationProvider: DeviceInformationProvider,
    protected val metadataProvider: MetadataProvider,
) : PlayerAdapter {
    protected abstract val eventDataManipulators: Collection<EventDataManipulator>

    override val isAutoplayEnabled: Boolean? = null

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        eventDataManipulators.forEach { eventDataFactory.registerEventDataManipulator(it) }
        return featureFactory.createFeatures()
    }

    override fun createEventData() =
        eventDataFactory.create(
            stateMachine.impressionId,
            getCurrentSourceMetadata(),
            defaultMetadata,
            deviceInformationProvider.getDeviceInformation(),
            playerInfo,
        )

    override fun createEventDataForCustomDataEvent(sourceMetadata: SourceMetadata) =
        eventDataFactory.create(
            stateMachine.impressionId,
            sourceMetadata,
            defaultMetadata,
            deviceInformationProvider.getDeviceInformation(),
            playerInfo,
        )

    override fun release() {
        eventDataFactory.clearEventDataManipulators()
        stateMachine.release()
    }

    override var defaultMetadata: DefaultMetadata
        get() = metadataProvider.defaultMetadata
        set(value) {
            metadataProvider.defaultMetadata = value
        }

    override fun getCurrentSourceMetadata(): SourceMetadata {
        return metadataProvider.getSourceMetadata() ?: SourceMetadata()
    }
}
