package com.bitmovin.analytics.data.cache

import com.bitmovin.analytics.TestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SourceDataCacheTest {
    @Test
    fun `fromEventData maps all fields correctly`() {
        val eventData =
            TestFactory.createEventData().apply {
                mpdUrl = "https://example.com/stream.mpd"
                m3u8Url = "https://example.com/stream.m3u8"
                progUrl = "https://example.com/stream.mp4"
                isLive = true
                autoplay = true
                isCasting = true
                castTech = "Chromecast"
                videoDuration = 120000L
                videoPlaybackWidth = 1920
                videoPlaybackHeight = 1080
                videoBitrate = 5000000
                audioBitrate = 128000
                videoTimeEnd = 60000L
                streamFormat = "hls"
                videoCodec = "avc1.64001F"
                audioCodec = "mp4a.40.2"
                subtitleEnabled = true
                subtitleLanguage = "en"
                audioLanguage = "de"
            }

        val cache = SourceDataCache.fromEventData(eventData)

        assertThat(cache.mpdUrl).isEqualTo("https://example.com/stream.mpd")
        assertThat(cache.m3u8Url).isEqualTo("https://example.com/stream.m3u8")
        assertThat(cache.progUrl).isEqualTo("https://example.com/stream.mp4")
        assertThat(cache.isLive).isTrue()
        assertThat(cache.isCasting).isTrue()
        assertThat(cache.castTech).isEqualTo("Chromecast")
        assertThat(cache.videoDuration).isEqualTo(120000L)
        assertThat(cache.videoPlaybackWidth).isEqualTo(1920)
        assertThat(cache.videoPlaybackHeight).isEqualTo(1080)
        assertThat(cache.videoBitrate).isEqualTo(5000000)
        assertThat(cache.audioBitrate).isEqualTo(128000)
        assertThat(cache.videoTimeEnd).isEqualTo(60000L)
        assertThat(cache.streamFormat).isEqualTo("hls")
        assertThat(cache.videoCodec).isEqualTo("avc1.64001F")
        assertThat(cache.audioCodec).isEqualTo("mp4a.40.2")
        assertThat(cache.subtitleEnabled).isTrue()
        assertThat(cache.subtitleLanguage).isEqualTo("en")
        assertThat(cache.audioLanguage).isEqualTo("de")
    }

    @Test
    fun `fromEventData maps null optional fields correctly`() {
        val eventData =
            TestFactory.createEventData().apply {
                mpdUrl = null
                m3u8Url = null
                progUrl = null
                autoplay = null
                castTech = null
                videoCodec = null
                audioCodec = null
                subtitleLanguage = null
                audioLanguage = null
                streamFormat = null
            }

        val cache = SourceDataCache.fromEventData(eventData)

        assertThat(cache.mpdUrl).isNull()
        assertThat(cache.m3u8Url).isNull()
        assertThat(cache.progUrl).isNull()
        assertThat(cache.castTech).isNull()
        assertThat(cache.videoCodec).isNull()
        assertThat(cache.audioCodec).isNull()
        assertThat(cache.subtitleLanguage).isNull()
        assertThat(cache.audioLanguage).isNull()
        assertThat(cache.streamFormat).isNull()
    }
}
