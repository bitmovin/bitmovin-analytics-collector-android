package com.bitmovin.analytics.data

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline
import com.bitmovin.analytics.utils.UserAgentProvider

class EventDataFactory(
    private val config: AnalyticsConfig,
    private val userIdProvider: UserIdProvider,
    private val userAgentProvider: UserAgentProvider,
) : EventDataManipulatorPipeline {
    private val eventDataManipulators = mutableListOf<EventDataManipulator>()

    fun create(impressionId: String, sourceMetadata: SourceMetadata?, defaultMetadata: DefaultMetadata, deviceInformation: DeviceInformation, playerInfo: PlayerInfo): EventData {
        // TODO: refactor to put logic for merging sourceMetadata and defaultmetadata customData into separate method
        // and use customData as parameter of eventData
        val eventData = EventData(
            deviceInformation,
            playerInfo,
            impressionId,
            userIdProvider.userId(),
            config.licenseKey,
            null, // TODO: can we get rid of the player key constructor parameter?
            sourceMetadata?.videoId,
            sourceMetadata?.title,
            defaultMetadata?.customUserId,
            // TODO: put this merging into separate method and only add customdata here in the constructor
            if (sourceMetadata?.customData?.customData1 == null) defaultMetadata?.customData?.customData1 else sourceMetadata.customData.customData1,
            if (sourceMetadata?.customData?.customData2 == null) defaultMetadata?.customData?.customData2 else sourceMetadata.customData.customData2,
            if (sourceMetadata?.customData?.customData3 == null) defaultMetadata?.customData?.customData3 else sourceMetadata.customData.customData3,
            if (sourceMetadata?.customData?.customData4 == null) defaultMetadata?.customData?.customData4 else sourceMetadata.customData.customData4,
            if (sourceMetadata?.customData?.customData5 == null) defaultMetadata?.customData?.customData5 else sourceMetadata.customData.customData5,
            if (sourceMetadata?.customData?.customData6 == null) defaultMetadata?.customData?.customData6 else sourceMetadata.customData.customData6,
            if (sourceMetadata?.customData?.customData7 == null) defaultMetadata?.customData?.customData7 else sourceMetadata.customData.customData7,
            if (sourceMetadata?.customData?.customData8 == null) defaultMetadata?.customData?.customData8 else sourceMetadata.customData.customData8,
            if (sourceMetadata?.customData?.customData9 == null) defaultMetadata?.customData?.customData9 else sourceMetadata.customData.customData9,
            if (sourceMetadata?.customData?.customData10 == null) defaultMetadata?.customData?.customData10 else sourceMetadata.customData.customData10,
            if (sourceMetadata?.customData?.customData11 == null) defaultMetadata?.customData?.customData11 else sourceMetadata.customData.customData11,
            if (sourceMetadata?.customData?.customData12 == null) defaultMetadata?.customData?.customData12 else sourceMetadata.customData.customData12,
            if (sourceMetadata?.customData?.customData13 == null) defaultMetadata?.customData?.customData13 else sourceMetadata.customData.customData13,
            if (sourceMetadata?.customData?.customData14 == null) defaultMetadata?.customData?.customData14 else sourceMetadata.customData.customData14,
            if (sourceMetadata?.customData?.customData15 == null) defaultMetadata?.customData?.customData15 else sourceMetadata.customData.customData15,
            if (sourceMetadata?.customData?.customData16 == null) defaultMetadata?.customData?.customData16 else sourceMetadata.customData.customData16,
            if (sourceMetadata?.customData?.customData17 == null) defaultMetadata?.customData?.customData17 else sourceMetadata.customData.customData17,
            if (sourceMetadata?.customData?.customData18 == null) defaultMetadata?.customData?.customData18 else sourceMetadata.customData.customData18,
            if (sourceMetadata?.customData?.customData19 == null) defaultMetadata?.customData?.customData19 else sourceMetadata.customData.customData19,
            if (sourceMetadata?.customData?.customData20 == null) defaultMetadata?.customData?.customData20 else sourceMetadata.customData.customData20,
            if (sourceMetadata?.customData?.customData21 == null) defaultMetadata?.customData?.customData21 else sourceMetadata.customData.customData21,
            if (sourceMetadata?.customData?.customData22 == null) defaultMetadata?.customData?.customData22 else sourceMetadata.customData.customData22,
            if (sourceMetadata?.customData?.customData23 == null) defaultMetadata?.customData?.customData23 else sourceMetadata.customData.customData23,
            if (sourceMetadata?.customData?.customData24 == null) defaultMetadata?.customData?.customData24 else sourceMetadata.customData.customData24,
            if (sourceMetadata?.customData?.customData25 == null) defaultMetadata?.customData?.customData25 else sourceMetadata.customData.customData25,
            if (sourceMetadata?.customData?.customData26 == null) defaultMetadata?.customData?.customData26 else sourceMetadata.customData.customData26,
            if (sourceMetadata?.customData?.customData27 == null) defaultMetadata?.customData?.customData27 else sourceMetadata.customData.customData27,
            if (sourceMetadata?.customData?.customData28 == null) defaultMetadata?.customData?.customData28 else sourceMetadata.customData.customData28,
            if (sourceMetadata?.customData?.customData29 == null) defaultMetadata?.customData?.customData29 else sourceMetadata.customData.customData29,
            if (sourceMetadata?.customData?.customData30 == null) defaultMetadata?.customData?.customData30 else sourceMetadata.customData.customData30,
            sourceMetadata?.path,
            if (sourceMetadata?.customData?.experimentName == null) defaultMetadata?.customData?.experimentName else sourceMetadata.customData.experimentName,
            if (sourceMetadata?.cdnProvider == null) defaultMetadata?.cdnProvider else sourceMetadata.cdnProvider,
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
