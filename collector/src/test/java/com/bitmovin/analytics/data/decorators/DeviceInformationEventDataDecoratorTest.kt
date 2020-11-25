package com.bitmovin.analytics.data.decorators

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventData
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class DeviceInformationEventDataDecoratorTest {

    private val licenseKey = UUID.randomUUID().toString()
    private val impressionId = UUID.randomUUID().toString()
    private val userId = UUID.randomUUID().toString()

    @Test
    fun testDecorateSetsDeviceInformationOnEventData() {
        // #region Mocking
        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
        val deviceInformationProviderMock = Mockito.mock(DeviceInformationProvider::class.java)

        val deviceInformation = DeviceInformation("myManufacturer", "myModel", false, "user-agent", "de", "package-name", 100, 200)
        `when`(deviceInformationProviderMock.getDeviceInformation()).thenReturn(deviceInformation)

        val eventData = EventData(bitmovinAnalyticsConfigMock, impressionId, userId)
        // #endregion

        val decorator = DeviceInformationEventDataDecorator(deviceInformationProviderMock)
        decorator.decorate(eventData)

        assertThat(eventData.deviceInformation?.manufacturer).isEqualTo("myManufacturer")
        assertThat(eventData.deviceInformation?.model).isEqualTo("myModel")
        assertThat(eventData.deviceInformation?.isTV).isFalse()
        assertThat(eventData.userAgent).isEqualTo("user-agent")
        assertThat(eventData.language).isEqualTo("de")
        assertThat(eventData.domain).isEqualTo("package-name")
        assertThat(eventData.screenHeight).isEqualTo(100)
        assertThat(eventData.screenWidth).isEqualTo(200)
        assertThat(eventData.platform).isEqualTo("android")
    }

    @Test
    fun testDecorateSetsDeviceInformationOnEventDataForAndroidTV() {
        // #region Mocking
        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
        val deviceInformationProviderMock = Mockito.mock(DeviceInformationProvider::class.java)

        val deviceInformation = DeviceInformation("myManufacturer", "myModel", true, "user-agent", "de", "package-name", 100, 200)
        `when`(deviceInformationProviderMock.getDeviceInformation()).thenReturn(deviceInformation)

        val eventData = EventData(bitmovinAnalyticsConfigMock, impressionId, userId)
        // #endregion

        val decorator = DeviceInformationEventDataDecorator(deviceInformationProviderMock)
        decorator.decorate(eventData)

        assertThat(eventData.platform).isEqualTo("androidTV")
    }

    @Test
    fun testDecorateDoesNotChangeImpressionID() {
        // #region Mocking
        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
        val deviceInformationProviderMock = Mockito.mock(DeviceInformationProvider::class.java)

        val deviceInformation = DeviceInformation("myManufacturer", "myModel", true, "user-agent", "de", "package-name", 100, 200)
        `when`(deviceInformationProviderMock.getDeviceInformation()).thenReturn(deviceInformation)

        val eventData = EventData(bitmovinAnalyticsConfigMock, impressionId, userId)
        // #endregion

        val decorator = DeviceInformationEventDataDecorator(deviceInformationProviderMock)
        decorator.decorate(eventData)

        assertThat(eventData.impressionId).isEqualTo(impressionId)
    }

    @Test
    fun testDecorateDoesNotChangeUserID() {
        // #region Mocking
        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
        val deviceInformationProviderMock = Mockito.mock(DeviceInformationProvider::class.java)

        val deviceInformation = DeviceInformation("myManufacturer", "myModel", true, "user-agent", "de", "package-name", 100, 200)
        `when`(deviceInformationProviderMock.getDeviceInformation()).thenReturn(deviceInformation)

        val eventData = EventData(bitmovinAnalyticsConfigMock, impressionId, userId)
        // #endregion

        val decorator = DeviceInformationEventDataDecorator(deviceInformationProviderMock)
        decorator.decorate(eventData)

        assertThat(eventData.userId).isEqualTo(userId)
    }
}
