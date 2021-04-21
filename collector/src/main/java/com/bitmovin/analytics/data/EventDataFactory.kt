package com.bitmovin.analytics.data

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline


class EventDataFactory(private val config: BitmovinAnalyticsConfig, private val userIdProvider: UserIdProvider): EventDataManipulatorPipeline {
    private val eventDataManipulators = mutableListOf<EventDataManipulator>()

    // TODO DeviceInformationProvider for now is only available after `attachPlayerAdapter`, but can also be moved to the constructor of BitmovinAnalytics and also in this class
    fun create(impressionId: String, sourceMetadata: SourceMetadata?, deviceInformationProvider: DeviceInformationProvider): EventData {
        val eventData = EventData(
                deviceInformationProvider.getDeviceInformation(),
                impressionId,
                userIdProvider.userId(),
                config.key,
                config.playerKey,
                if (sourceMetadata == null) config.videoId else sourceMetadata.videoId
                if (sourceMetadata == null) config.title else sourceMetadata.title,
                config.customUserId,
                if (sourceMetadata == null) config.customData1 else sourceMetadata.customData1,
                if (sourceMetadata == null) config.customData2 else sourceMetadata.customData2,
                if (sourceMetadata == null) config.customData3 else sourceMetadata.customData3,
                if (sourceMetadata == null) config.customData4 else sourceMetadata.customData4,
                if (sourceMetadata == null) config.customData5 else sourceMetadata.customData5,
                if (sourceMetadata == null) config.customData6 else sourceMetadata.customData6,
                if (sourceMetadata == null) config.customData7 else sourceMetadata.customData7,
                if (sourceMetadata == null) config.path else sourceMetadata.path,
                if (sourceMetadata == null) config.experimentName else sourceMetadata.experimentName,
                if (sourceMetadata == null) config.cdnProvider else sourceMetadata.cdnProvider,
                /*TODO This will always be overridden in the adapters, we need a logic like with m3u8 url*/
                config.playerType?.toString())

        for (decorator in eventDataManipulators) {
            decorator.manipulate(eventData)
        }

        return eventData
    }

    override fun clearEventDataManipulators() {
        eventDataManipulators.clear()
    }

    override fun registerEventDataManipulator(manipulator: EventDataManipulator) {
        eventDataManipulators.add(manipulator)
    }
}
