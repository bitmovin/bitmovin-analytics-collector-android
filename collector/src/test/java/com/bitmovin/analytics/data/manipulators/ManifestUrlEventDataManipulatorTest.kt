package com.bitmovin.analytics.data.manipulators

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventData
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ManifestUrlEventDataManipulatorTest {
    private val licenseKey = UUID.randomUUID().toString()
    private val impressionId = UUID.randomUUID().toString()
    private val userId = UUID.randomUUID().toString()

    private lateinit var deviceInformation: DeviceInformation

    @Before
    fun setup() {
        deviceInformation = DeviceInformation("myManufacturer", "myModel", false, "user-agent", "de", "package-name", 100, 200)
    }

    @Test
    fun testManipulateSetsM3u8Url() {
        // #region Mocking
        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
        bitmovinAnalyticsConfigMock.m3u8Url = "https://www.my-domain.com/file.m3u8"

        val eventData = EventData(bitmovinAnalyticsConfigMock, deviceInformation, impressionId, userId)
        // #endregion

        val manipulator = ManifestUrlEventDataManipulator(bitmovinAnalyticsConfigMock)
        manipulator.manipulate(eventData)

        Assertions.assertThat(eventData.m3u8Url).isEqualTo("https://www.my-domain.com/file.m3u8")
        Assertions.assertThat(eventData.mpdUrl).isNull()
    }

    @Test
    fun testManipulateSetsMpdUrl() {
        // #region Mocking
        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
        bitmovinAnalyticsConfigMock.mpdUrl = "https://www.my-domain.com/file.mpd"

        val eventData = EventData(bitmovinAnalyticsConfigMock, deviceInformation, impressionId, userId)
        // #endregion

        val manipulator = ManifestUrlEventDataManipulator(bitmovinAnalyticsConfigMock)
        manipulator.manipulate(eventData)

        Assertions.assertThat(eventData.mpdUrl).isEqualTo("https://www.my-domain.com/file.mpd")
        Assertions.assertThat(eventData.m3u8Url).isNull()
    }
}
