package com.bitmovin.analytics

import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.utils.DataSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DeviceInformationDtoDataSerializationTest {
    @Test
    fun testSerializesEventDataDeviceInformationCorrectly() {
        val config = BitmovinAnalyticsConfig("9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac", "18ca6ad5-9768-4129-bdf6-17685e0d14d2")
        val model = "Pixel 3"
        val manufacturer = "Google"
        val data = EventData(config, "1234", DeviceInformation(manufacturer, model, "user-agent", "de", "my.package", 100, 200), "user-id")
        val serialized = DataSerializer.serialize(data)

        assertThat(serialized).contains("\"deviceInformation\":{")
        assertThat(serialized).contains(String.format("\"model\":\"%s\"", data.deviceInformation.model))
        assertThat(serialized).contains(String.format("\"manufacturer\":\"%s\"", data.deviceInformation.manufacturer))
    }
}
