package com.bitmovin.analytics.data.cache

import com.bitmovin.analytics.TestFactory
import com.bitmovin.analytics.internal.InternalBitmovinApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@OptIn(InternalBitmovinApi::class)
class CacheServiceTest {
    private lateinit var cacheService: CacheService

    @Before
    fun setup() {
        cacheService = CacheService()
    }

    @Test
    fun `applyCacheOnEventData does nothing when cache is empty`() {
        val eventData =
            TestFactory.createEventData().apply {
                mpdUrl = "original-mpd"
                m3u8Url = "original-m3u8"
                videoTimeEnd = 1000L
            }

        cacheService.applyCacheOnEventData(eventData)

        assertThat(eventData.mpdUrl).isEqualTo("original-mpd")
        assertThat(eventData.m3u8Url).isEqualTo("original-m3u8")
        assertThat(eventData.videoTimeEnd).isEqualTo(1000L)
    }

    @Test
    fun `setSourceCacheFromEventData and applyCacheOnEventData overwrites all source fields`() {
        val sourceEventData =
            TestFactory.createEventData().apply {
                mpdUrl = "cached-mpd"
                m3u8Url = "cached-m3u8"
                progUrl = "cached-prog"
                isLive = true
                isCasting = true
                castTech = "Chromecast"
                videoDuration = 90000L
                videoPlaybackWidth = 1280
                videoPlaybackHeight = 720
                videoBitrate = 3000000
                audioBitrate = 64000
                streamFormat = "hls"
                videoCodec = "avc1"
                audioCodec = "mp4a"
                subtitleEnabled = true
                subtitleLanguage = "fr"
                audioLanguage = "es"
                videoTimeEnd = 5000L
            }
        cacheService.setSourceCacheFromEventData(sourceEventData)

        val targetEventData =
            TestFactory.createEventData().apply {
                mpdUrl = "old-mpd"
                m3u8Url = "old-m3u8"
                progUrl = "old-prog"
                isLive = false
                videoTimeEnd = 1000L
            }
        cacheService.applyCacheOnEventData(targetEventData)

        assertThat(targetEventData.mpdUrl).isEqualTo("cached-mpd")
        assertThat(targetEventData.m3u8Url).isEqualTo("cached-m3u8")
        assertThat(targetEventData.progUrl).isEqualTo("cached-prog")
        assertThat(targetEventData.isLive).isTrue()
        assertThat(targetEventData.isCasting).isTrue()
        assertThat(targetEventData.castTech).isEqualTo("Chromecast")
        assertThat(targetEventData.videoDuration).isEqualTo(90000L)
        assertThat(targetEventData.videoPlaybackWidth).isEqualTo(1280)
        assertThat(targetEventData.videoPlaybackHeight).isEqualTo(720)
        assertThat(targetEventData.videoBitrate).isEqualTo(3000000)
        assertThat(targetEventData.audioBitrate).isEqualTo(64000)
        assertThat(targetEventData.streamFormat).isEqualTo("hls")
        assertThat(targetEventData.videoCodec).isEqualTo("avc1")
        assertThat(targetEventData.audioCodec).isEqualTo("mp4a")
        assertThat(targetEventData.subtitleEnabled).isTrue()
        assertThat(targetEventData.subtitleLanguage).isEqualTo("fr")
        assertThat(targetEventData.audioLanguage).isEqualTo("es")
    }

    @Test
    fun `setSourceCacheFromEventData stores videoTimeEnd in dynamic cache`() {
        val sourceEventData =
            TestFactory.createEventData().apply {
                videoTimeEnd = 8000L
            }
        cacheService.setSourceCacheFromEventData(sourceEventData)

        val targetEventData =
            TestFactory.createEventData().apply {
                videoTimeEnd = 5000L
            }
        cacheService.applyCacheOnEventData(targetEventData)

        assertThat(targetEventData.videoTimeEnd).isEqualTo(8000L)
    }

    @Test
    fun `applyCacheOnEventData does not overwrite videoTimeEnd when cached value is lower`() {
        val sourceEventData =
            TestFactory.createEventData().apply {
                videoTimeEnd = 3000L
            }
        cacheService.setSourceCacheFromEventData(sourceEventData)

        val targetEventData =
            TestFactory.createEventData().apply {
                videoTimeEnd = 5000L
            }
        cacheService.applyCacheOnEventData(targetEventData)

        assertThat(targetEventData.videoTimeEnd).isEqualTo(5000L)
    }

    @Test
    fun `setVideoTimeEnd updates dynamic cache and is applied when greater than eventData value`() {
        cacheService.setVideoTimeEnd(9000L)

        val eventData =
            TestFactory.createEventData().apply {
                videoTimeEnd = 5000L
            }
        cacheService.applyCacheOnEventData(eventData)

        assertThat(eventData.videoTimeEnd).isEqualTo(9000L)
    }

    @Test
    fun `setVideoTimeEnd does not override eventData videoTimeEnd when lower`() {
        cacheService.setVideoTimeEnd(2000L)

        val eventData =
            TestFactory.createEventData().apply {
                videoTimeEnd = 5000L
            }
        cacheService.applyCacheOnEventData(eventData)

        assertThat(eventData.videoTimeEnd).isEqualTo(5000L)
    }

    @Test
    fun `resetSourceCache clears source fields and dynamic cache`() {
        val sourceEventData =
            TestFactory.createEventData().apply {
                mpdUrl = "cached-mpd"
                videoTimeEnd = 8000L
            }
        cacheService.setSourceCacheFromEventData(sourceEventData)
        cacheService.resetSourceCache()

        val eventData =
            TestFactory.createEventData().apply {
                mpdUrl = "original-mpd"
                videoTimeEnd = 3000L
            }
        cacheService.applyCacheOnEventData(eventData)

        assertThat(eventData.mpdUrl).isEqualTo("original-mpd")
        assertThat(eventData.videoTimeEnd).isEqualTo(3000L)
    }
}
