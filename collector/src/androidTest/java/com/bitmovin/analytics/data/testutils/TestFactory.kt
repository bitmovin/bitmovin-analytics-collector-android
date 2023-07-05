package com.bitmovin.analytics.data.testutils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.utils.ApiV3Utils
import com.bitmovin.analytics.utils.Util
import java.util.UUID

object TestFactory {
    private val testDeviceInformation = DeviceInformation(
        manufacturer = "manufacturer",
        model = "model",
        isTV = false,
        locale = "locale",
        domain = "packageName",
        screenHeight = 2400,
        screenWidth = 1080,
    )

    fun createAdEventData(
        adId: String = "testAdId",
        videoImpressionId: String,
        adImpressionId: String = UUID.randomUUID().toString(),
        playerInfo: PlayerInfo = PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
        time: Long? = null,
    ) = AdEventData(
        adId = adId,
        videoImpressionId = videoImpressionId,
        adImpressionId = adImpressionId,
        userAgent = "userAgent",
        domain = "bitmovin.com",
        language = "en",
        player = playerInfo.playerType.toString(),
        screenHeight = 9,
        screenWidth = 16,
        platform = "android",
        playerTech = playerInfo.playerTech,
        videoWindowHeight = 9,
        videoWindowWidth = 16,
        userId = "testUser",
        time = time ?: Util.timestamp,
    )

    fun createEventData(
        impressionId: String,
        sequenceNumber: Int? = null,
        userId: String = "testUser",
        config: BitmovinAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(title = "LongTermRetryOnFailureTest"),
        userAgent: String = "testingUserAgent",
        sourceMetadata: SourceMetadata = SourceMetadata(),
        deviceInformation: DeviceInformation = testDeviceInformation,
        playerInfo: PlayerInfo = PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
        time: Long? = null,
    ): EventData {
        val defaultMetadata = ApiV3Utils.extractDefaultMetadata(config)
        val mergedCustomData = ApiV3Utils.mergeCustomData(sourceMetadata.customData, defaultMetadata.customData)

        return EventData(
            deviceInformation,
            playerInfo,
            mergedCustomData,
            impressionId,
            userId,
            config.key,
            if (sourceMetadata.videoId == null) config.videoId else sourceMetadata.videoId,
            if (sourceMetadata.title == null) config.title else sourceMetadata.title,
            config.customUserId,
            if (sourceMetadata.path == null) config.path else sourceMetadata.path,
            if (sourceMetadata.cdnProvider == null) config.cdnProvider else sourceMetadata.cdnProvider,
            userAgent,
        ).apply {
            if (sequenceNumber != null) this.sequenceNumber = sequenceNumber
            if (time != null) this.time = time
        }
    }
}
