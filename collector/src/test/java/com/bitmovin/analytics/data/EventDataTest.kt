package com.bitmovin.analytics.data

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.TestFactory
import com.bitmovin.analytics.enums.PlayerType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.UUID

class EventDataTest {
    private val playerKey = UUID.randomUUID().toString()
    private val licenseKey = UUID.randomUUID().toString()
    private val impressionId = UUID.randomUUID().toString()
    private val userId = UUID.randomUUID().toString()

    private lateinit var bitmovinAnalyticsConfig: BitmovinAnalyticsConfig

    @Before
    fun setup() {
        bitmovinAnalyticsConfig = BitmovinAnalyticsConfig(licenseKey, playerKey)
    }

    @Test
    fun testEventDataContainsDeviceInformation() {
        val deviceInformation =
            DeviceInformation("myManufacturer", "myModel", false, "de", "package-name", 100, 200)
        val eventData = TestFactory.createEventDataFactory(bitmovinAnalyticsConfig).create(
            impressionId,
            null,
            deviceInformation,
            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
        )

        assertThat(eventData.deviceInformation.manufacturer).isEqualTo("myManufacturer")
        assertThat(eventData.deviceInformation.model).isEqualTo("myModel")
        assertThat(eventData.deviceInformation.isTV).isFalse()
        assertThat(eventData.language).isEqualTo("de")
        assertThat(eventData.domain).isEqualTo("package-name")
        assertThat(eventData.screenHeight).isEqualTo(100)
        assertThat(eventData.screenWidth).isEqualTo(200)
        assertThat(eventData.platform).isEqualTo("android")
    }

    @Test
    fun testEventDataSetsPlatformToAndroidTVIfDeviceInformationIsTVIsTrue() {
        val deviceInformation =
            DeviceInformation("myManufacturer", "myModel", true, "de", "package-name", 100, 200)
        val eventData = TestFactory.createEventDataFactory(bitmovinAnalyticsConfig).create(
            impressionId,
            null,
            deviceInformation,
            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
        )

        assertThat(eventData.platform).isEqualTo("androidTV")
    }

    @Test
    fun testEventDataSetsRandomizedUserId() {
        val deviceInformation =
            DeviceInformation("myManufacturer", "myModel", true, "de", "package-name", 100, 200)
        var randomizedUserIdProvider = RandomizedUserIdIdProvider()
        var randomizedUserIdProvider1 = RandomizedUserIdIdProvider()
        var eventData = TestFactory.createEventDataFactory(
            bitmovinAnalyticsConfig,
            randomizedUserIdProvider,
        ).create(
            impressionId,
            null,
            deviceInformation,
            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
        )
        var eventData1 = TestFactory.createEventDataFactory(
            bitmovinAnalyticsConfig,
            randomizedUserIdProvider,
        ).create(
            impressionId,
            null,
            deviceInformation,
            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
        )
        var eventData2 = TestFactory.createEventDataFactory(
            bitmovinAnalyticsConfig,
            randomizedUserIdProvider1,
        ).create(
            impressionId,
            null,
            deviceInformation,
            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
        )

        assertThat(eventData.userId).isEqualTo(eventData1.userId)
        assertThat(eventData.userId).isEqualTo(randomizedUserIdProvider.userId())
        assertThat(eventData2.userId).isEqualTo(randomizedUserIdProvider1.userId())
        assertThat(eventData2.userId).isNotEqualTo(eventData1.userId)
    }
}
