package com.bitmovin.analytics

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.utils.DataSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DeviceInformationDtoDataSerializationTest {
    @Test
    fun testSerializesEventDataDeviceInformationCorrectly() {
        // #region Mocking
        val config = AnalyticsConfig(
            "9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac",
        )

        val deviceInformation =
            DeviceInformation("myManufacturer", "myModel", false, "de", "package-name", 100, 200)
        // #endregion

        val data = TestFactory.createEventDataFactory(config).create(
            "null",
            SourceMetadata(),
            DefaultMetadata(),
            deviceInformation,
            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
        )
        val serialized = DataSerializer.serialize(data)

        assertThat(serialized).contains("\"deviceInformation\":{")
        assertThat(serialized).contains(String.format("\"model\":\"%s\"", "myModel"))
        assertThat(serialized).contains(String.format("\"manufacturer\":\"%s\"", "myManufacturer"))
        assertThat(serialized).contains(String.format("\"platform\":\"%s\"", "android"))
        assertThat(serialized).contains(String.format("\"language\":\"%s\"", "de"))
        assertThat(serialized).contains(String.format("\"domain\":\"%s\"", "package-name"))
        assertThat(serialized).contains(String.format("\"isTV\":%s", false))
        assertThat(serialized).contains(String.format("\"screenHeight\":%s", 100))
        assertThat(serialized).contains(String.format("\"screenWidth\":%s", 200))
    }
}
