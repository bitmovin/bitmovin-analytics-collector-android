package com.bitmovin.analytics

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.data.UserIdProvider
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.utils.UserAgentProvider
import io.mockk.mockk

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

    fun createEventDataFactory(
        config: AnalyticsConfig,
        userIdProvider: UserIdProvider? = null,
        userAgentProvider: UserAgentProvider? = null,
    ): EventDataFactory {
        return EventDataFactory(
            config,
            userIdProvider ?: mockk(relaxed = true),
            userAgentProvider ?: mockk(relaxed = true),
        )
    }

    fun createEventData(
        config: AnalyticsConfig = AnalyticsConfig("test-key"),
        impressionId: String = "test-impression",
        sourceMetadata: SourceMetadata? = null,
        defaultMetadata: DefaultMetadata = DefaultMetadata(),
        deviceInformation: DeviceInformation = testDeviceInformation,
        playerInfo: PlayerInfo = PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
    ): EventData = createEventDataFactory(config).create(
        impressionId,
        sourceMetadata,
        defaultMetadata,
        deviceInformation,
        playerInfo,
    )

    fun createAdEventData(adId: String = "testAdId") = AdEventData(
        adId = adId,
        videoImpressionId = "video-impression-id",
        userAgent = "userAgent",
        domain = "bitmovin.com",
        language = "en",
        player = "bitmovin player",
        screenHeight = 9,
        screenWidth = 16,
        platform = "android",
        playerTech = "player tech",
        videoWindowHeight = 9,
        videoWindowWidth = 16,
        userId = "testUser",
    )
}
