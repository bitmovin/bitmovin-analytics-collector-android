package com.bitmovin.analytics.data

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.data.manipulators.EventDataManipulatorPipeline
import com.bitmovin.analytics.license.InstantLicenseKeyProvider
import com.bitmovin.analytics.license.LicenseKeyProvider
import com.bitmovin.analytics.license.licenseKeyOrNull
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.utils.ApiV3Utils
import com.bitmovin.analytics.utils.UserAgentProvider

class EventDataFactory(
    private val config: AnalyticsConfig,
    private val userIdProvider: UserIdProvider,
    private val userAgentProvider: UserAgentProvider,
    private val licenseKeyProvider: LicenseKeyProvider = InstantLicenseKeyProvider(config.licenseKey),
    private val ssaiService: SsaiService,
) : EventDataManipulatorPipeline {
    private val eventDataManipulators = mutableListOf<EventDataManipulator>()

    fun create(
        impressionId: String,
        sourceMetadata: SourceMetadata,
        defaultMetadata: DefaultMetadata,
        deviceInformation: DeviceInformation,
        playerInfo: PlayerInfo,
    ): EventData {
        var mergedCustomData = ApiV3Utils.mergeCustomData(sourceMetadata.customData, defaultMetadata.customData)
        mergedCustomData = ApiV3Utils.mergeCustomData(ssaiService.adMetadata?.customData, mergedCustomData)
        val mergedCdnProvider = sourceMetadata.cdnProvider ?: defaultMetadata.cdnProvider

        val eventData =
            EventData(
                deviceInformation,
                playerInfo,
                mergedCustomData,
                impressionId,
                userIdProvider.userId(),
                licenseKeyProvider.licenseKeyOrNull,
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
