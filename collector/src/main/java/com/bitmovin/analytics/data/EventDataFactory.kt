package com.bitmovin.analytics.data

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline
import com.bitmovin.analytics.utils.ApiV3Utils
import com.bitmovin.analytics.utils.UserAgentProvider

class EventDataFactory(
    private val config: AnalyticsConfig,
    private val userIdProvider: UserIdProvider,
    private val userAgentProvider: UserAgentProvider,
) : EventDataManipulatorPipeline {
    private val eventDataManipulators = mutableListOf<EventDataManipulator>()

    fun create(impressionId: String, sourceMetadata: SourceMetadata, defaultMetadata: DefaultMetadata, deviceInformation: DeviceInformation, playerInfo: PlayerInfo): EventData {
        val mergedCustomData = ApiV3Utils.mergeCustomData(sourceMetadata.customData, defaultMetadata.customData)
        val mergedCdnProvider = if (sourceMetadata.cdnProvider == null) defaultMetadata.cdnProvider else sourceMetadata.cdnProvider

        val eventData = EventData(
            deviceInformation,
            playerInfo,
            mergedCustomData,
            impressionId,
            userIdProvider.userId(),
            config.licenseKey,
            sourceMetadata.videoId,
            sourceMetadata.title,
            defaultMetadata.customUserId,
            sourceMetadata.path,
            mergedCdnProvider,
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
