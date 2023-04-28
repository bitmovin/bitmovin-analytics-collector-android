package com.bitmovin.analytics

import com.bitmovin.analytics.config.SourceMetadata
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
        screenHeight = 0,
        screenWidth = 0,
    )

    fun createEventDataFactory(
        config: BitmovinAnalyticsConfig,
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
        config: BitmovinAnalyticsConfig = BitmovinAnalyticsConfig(),
        impressionId: String = "test-impression",
        sourceMetadata: SourceMetadata? = null,
        deviceInformation: DeviceInformation = testDeviceInformation,
        playerInfo: PlayerInfo = PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
    ): EventData = createEventDataFactory(config).create(
        impressionId,
        sourceMetadata,
        deviceInformation,
        playerInfo,
    )

    fun createAdEventData(adId: String = "testAdId") = AdEventData(adId = adId)
}
