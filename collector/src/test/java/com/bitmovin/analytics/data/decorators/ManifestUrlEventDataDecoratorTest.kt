package com.bitmovin.analytics.data.decorators

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.EventData
import java.util.UUID
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ManifestUrlEventDataDecoratorTest {
    private val licenseKey = UUID.randomUUID().toString()
    private val impressionId = UUID.randomUUID().toString()
    private val userId = UUID.randomUUID().toString()

    @Test
    fun testDecorateSetsM3u8Url() {
        // #region Mocking
        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
        bitmovinAnalyticsConfigMock.m3u8Url = "https://www.my-domain.com/file.m3u8"

        val eventData = EventData(bitmovinAnalyticsConfigMock, impressionId, userId)
        // #endregion

        val decorator = ManifestUrlEventDataDecorator(bitmovinAnalyticsConfigMock)
        decorator.decorate(eventData)

        Assertions.assertThat(eventData.m3u8Url).isEqualTo("https://www.my-domain.com/file.m3u8")
        Assertions.assertThat(eventData.mpdUrl).isNull()
    }

    @Test
    fun testDecorateSetsMpdUrl() {
        // #region Mocking
        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
        bitmovinAnalyticsConfigMock.mpdUrl = "https://www.my-domain.com/file.mpd"

        val eventData = EventData(bitmovinAnalyticsConfigMock, impressionId, userId)
        // #endregion

        val decorator = ManifestUrlEventDataDecorator(bitmovinAnalyticsConfigMock)
        decorator.decorate(eventData)

        Assertions.assertThat(eventData.mpdUrl).isEqualTo("https://www.my-domain.com/file.mpd")
        Assertions.assertThat(eventData.m3u8Url).isNull()
    }
}
