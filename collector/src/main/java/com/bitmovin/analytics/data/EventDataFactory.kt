package com.bitmovin.analytics.data

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline
import com.bitmovin.analytics.utils.UserAgentProvider

class EventDataFactory(
    private val config: BitmovinAnalyticsConfig,
    private val userIdProvider: UserIdProvider,
    private val userAgentProvider: UserAgentProvider,
) : EventDataManipulatorPipeline {
    private val eventDataManipulators = mutableListOf<EventDataManipulator>()

    fun create(impressionId: String, sourceMetadata: SourceMetadata?, deviceInformation: DeviceInformation, playerInfo: PlayerInfo): EventData {
        val eventData = EventData(
            deviceInformation,
            playerInfo,
            impressionId,
            userIdProvider.userId(),
            config.key,
            config.playerKey,
            if (sourceMetadata == null) config.videoId else sourceMetadata.videoId,
            if (sourceMetadata == null) config.title else sourceMetadata.title,
            config.customUserId,
            if (sourceMetadata == null) config.customData1 else sourceMetadata.customData1,
            if (sourceMetadata == null) config.customData2 else sourceMetadata.customData2,
            if (sourceMetadata == null) config.customData3 else sourceMetadata.customData3,
            if (sourceMetadata == null) config.customData4 else sourceMetadata.customData4,
            if (sourceMetadata == null) config.customData5 else sourceMetadata.customData5,
            if (sourceMetadata == null) config.customData6 else sourceMetadata.customData6,
            if (sourceMetadata == null) config.customData7 else sourceMetadata.customData7,
            if (sourceMetadata == null) config.customData8 else sourceMetadata.customData8,
            if (sourceMetadata == null) config.customData9 else sourceMetadata.customData9,
            if (sourceMetadata == null) config.customData10 else sourceMetadata.customData10,
            if (sourceMetadata == null) config.customData11 else sourceMetadata.customData11,
            if (sourceMetadata == null) config.customData12 else sourceMetadata.customData12,
            if (sourceMetadata == null) config.customData13 else sourceMetadata.customData13,
            if (sourceMetadata == null) config.customData14 else sourceMetadata.customData14,
            if (sourceMetadata == null) config.customData15 else sourceMetadata.customData15,
            if (sourceMetadata == null) config.customData16 else sourceMetadata.customData16,
            if (sourceMetadata == null) config.customData17 else sourceMetadata.customData17,
            if (sourceMetadata == null) config.customData18 else sourceMetadata.customData18,
            if (sourceMetadata == null) config.customData19 else sourceMetadata.customData19,
            if (sourceMetadata == null) config.customData20 else sourceMetadata.customData20,
            if (sourceMetadata == null) config.customData21 else sourceMetadata.customData21,
            if (sourceMetadata == null) config.customData22 else sourceMetadata.customData22,
            if (sourceMetadata == null) config.customData23 else sourceMetadata.customData23,
            if (sourceMetadata == null) config.customData24 else sourceMetadata.customData24,
            if (sourceMetadata == null) config.customData25 else sourceMetadata.customData25,
            if (sourceMetadata == null) config.customData26 else sourceMetadata.customData26,
            if (sourceMetadata == null) config.customData27 else sourceMetadata.customData27,
            if (sourceMetadata == null) config.customData28 else sourceMetadata.customData28,
            if (sourceMetadata == null) config.customData29 else sourceMetadata.customData29,
            if (sourceMetadata == null) config.customData30 else sourceMetadata.customData30,

            if (sourceMetadata == null) config.path else sourceMetadata.path,
            if (sourceMetadata == null) config.experimentName else sourceMetadata.experimentName,
            if (sourceMetadata == null) config.cdnProvider else sourceMetadata.cdnProvider,
            userAgentProvider.userAgent,
        )

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
