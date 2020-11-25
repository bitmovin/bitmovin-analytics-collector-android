package com.bitmovin.analytics

import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.decorators.DeviceInformationEventDataDecorator
import com.bitmovin.analytics.utils.DataSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito

class DeviceInformationDtoDataSerializationTest {
    @Test
    fun testSerializesEventDataDeviceInformationCorrectly() {
        //#region Mocking
        val config = BitmovinAnalyticsConfig("9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac", "18ca6ad5-9768-4129-bdf6-17685e0d14d2")
        val data = EventData(config, "1234", "user-id")

        val deviceInformationProviderMock = Mockito.mock(DeviceInformationProvider::class.java)
        val deviceInformation = DeviceInformation("myManufacturer", "myModel", false, "user-agent", "de", "package-name", 100, 200)
        Mockito.`when`(deviceInformationProviderMock.getDeviceInformation()).thenReturn(deviceInformation)

        val decorator = DeviceInformationEventDataDecorator(deviceInformationProviderMock)
        decorator.decorate(data)

        val serialized = DataSerializer.serialize(data)
        //#endregion

        assertThat(serialized).contains("\"deviceInformation\":{")
        assertThat(serialized).contains(String.format("\"model\":\"%s\"", "myModel"))
        assertThat(serialized).contains(String.format("\"manufacturer\":\"%s\"", "myManufacturer"))
        assertThat(serialized).contains(String.format("\"userAgent\":\"%s\"", "user-agent"))
        assertThat(serialized).contains(String.format("\"platform\":\"%s\"", "android"))
        assertThat(serialized).contains(String.format("\"language\":\"%s\"", "de"))
        assertThat(serialized).contains(String.format("\"domain\":\"%s\"", "package-name"))
        assertThat(serialized).contains(String.format("\"isTV\":%s", false))
        assertThat(serialized).contains(String.format("\"screenHeight\":%s", 100))
        assertThat(serialized).contains(String.format("\"screenWidth\":%s", 200))
    }
}
