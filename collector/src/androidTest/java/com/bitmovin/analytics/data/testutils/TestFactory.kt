package com.bitmovin.analytics.data.testutils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.systemtest.utils.TestConfig
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
        sourceMetadata: SourceMetadata? = null,
        deviceInformation: DeviceInformation = testDeviceInformation,
        playerInfo: PlayerInfo = PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
        time: Long? = null,
    ): EventData {
        return EventData(
            deviceInformation,
            playerInfo,
            impressionId,
            userId,
            config.key,
            config.playerKey,
            if (sourceMetadata == null) config.videoId else sourceMetadata.videoId,
            if (sourceMetadata == null) config.title else sourceMetadata.title,
            config.customUserId,
            if (sourceMetadata == null) config.customData1 else sourceMetadata.customData.customData1,
            if (sourceMetadata == null) config.customData2 else sourceMetadata.customData.customData2,
            if (sourceMetadata == null) config.customData3 else sourceMetadata.customData.customData3,
            if (sourceMetadata == null) config.customData4 else sourceMetadata.customData.customData4,
            if (sourceMetadata == null) config.customData5 else sourceMetadata.customData.customData5,
            if (sourceMetadata == null) config.customData6 else sourceMetadata.customData.customData6,
            if (sourceMetadata == null) config.customData7 else sourceMetadata.customData.customData7,
            if (sourceMetadata == null) config.customData8 else sourceMetadata.customData.customData8,
            if (sourceMetadata == null) config.customData9 else sourceMetadata.customData.customData9,
            if (sourceMetadata == null) config.customData10 else sourceMetadata.customData.customData10,
            if (sourceMetadata == null) config.customData11 else sourceMetadata.customData.customData11,
            if (sourceMetadata == null) config.customData12 else sourceMetadata.customData.customData12,
            if (sourceMetadata == null) config.customData13 else sourceMetadata.customData.customData13,
            if (sourceMetadata == null) config.customData14 else sourceMetadata.customData.customData14,
            if (sourceMetadata == null) config.customData15 else sourceMetadata.customData.customData15,
            if (sourceMetadata == null) config.customData16 else sourceMetadata.customData.customData16,
            if (sourceMetadata == null) config.customData17 else sourceMetadata.customData.customData17,
            if (sourceMetadata == null) config.customData18 else sourceMetadata.customData.customData18,
            if (sourceMetadata == null) config.customData19 else sourceMetadata.customData.customData19,
            if (sourceMetadata == null) config.customData20 else sourceMetadata.customData.customData20,
            if (sourceMetadata == null) config.customData21 else sourceMetadata.customData.customData21,
            if (sourceMetadata == null) config.customData22 else sourceMetadata.customData.customData22,
            if (sourceMetadata == null) config.customData23 else sourceMetadata.customData.customData23,
            if (sourceMetadata == null) config.customData24 else sourceMetadata.customData.customData24,
            if (sourceMetadata == null) config.customData25 else sourceMetadata.customData.customData25,
            if (sourceMetadata == null) config.customData26 else sourceMetadata.customData.customData26,
            if (sourceMetadata == null) config.customData27 else sourceMetadata.customData.customData27,
            if (sourceMetadata == null) config.customData28 else sourceMetadata.customData.customData28,
            if (sourceMetadata == null) config.customData29 else sourceMetadata.customData.customData29,
            if (sourceMetadata == null) config.customData30 else sourceMetadata.customData.customData30,

            if (sourceMetadata == null) config.path else sourceMetadata.path,
            if (sourceMetadata == null) config.experimentName else sourceMetadata.customData.experimentName,
            if (sourceMetadata == null) config.cdnProvider else sourceMetadata.cdnProvider,
            userAgent,
        ).apply {
            if (sequenceNumber != null) this.sequenceNumber = sequenceNumber
            if (time != null) this.time = time
        }
    }
}
