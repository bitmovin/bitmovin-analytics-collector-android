package com.bitmovin.analytics.data

import androidx.test.runner.AndroidJUnit4
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
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
        val deviceInformation = DeviceInformation("myManufacturer", "myModel", false, "user-agent", "de", "package-name", 100, 200)
        val eventData = EventData(bitmovinAnalyticsConfig, deviceInformation, impressionId, userId)

        assertThat(eventData.deviceInformation.manufacturer).isEqualTo("myManufacturer")
        assertThat(eventData.deviceInformation.model).isEqualTo("myModel")
        assertThat(eventData.deviceInformation.isTV).isFalse()
        assertThat(eventData.userAgent).isEqualTo("user-agent")
        assertThat(eventData.language).isEqualTo("de")
        assertThat(eventData.domain).isEqualTo("package-name")
        assertThat(eventData.screenHeight).isEqualTo(100)
        assertThat(eventData.screenWidth).isEqualTo(200)
        assertThat(eventData.platform).isEqualTo("android")
    }

    @Test
    fun testEventDataSetsPlatformToAndroidTVIfDeviceInformationIsTVIsTrue() {
        val deviceInformation = DeviceInformation("myManufacturer", "myModel", true, "user-agent", "de", "package-name", 100, 200)
        val eventData = EventData(bitmovinAnalyticsConfig, deviceInformation, impressionId, userId)

        assertThat(eventData.platform).isEqualTo("androidTV")
    }
}
