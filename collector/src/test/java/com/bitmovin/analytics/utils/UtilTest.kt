package com.bitmovin.analytics.utils

import android.net.Uri
import androidx.core.net.toUri
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.TestFactory
import com.bitmovin.analytics.enums.StreamFormat
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(
    RobolectricTestRunner::class,
)
class UtilTest {
    @Test
    fun eventDataFormatAndUrlDetectionBasedOnExtension() {
        val progressiveMedias =
            listOf(
                Uri.parse("http://www.example.com/video.mp4"),
                Uri.parse("http://www.example.com/video.mp4?token=1234"),
                Uri.parse("http://www.example.com/video.mp4?token=1234&other=56.78"),
                Uri.parse("http://www.example.com/video.mp3"),
                Uri.parse("http://www.example.com/video.mkv"),
                Uri.parse("http://www.example.com/video.webm"),
                Uri.parse("http://www.example.com/video.m4a"),
            )
        val hlsMedias =
            listOf(
                Uri.parse("http://www.example.com/video.m3u8"),
                Uri.parse("http://www.example.com/video.m3u8?token=1234"),
                Uri.parse("http://www.example.com/video.m3u8?token=1234&other=5678"),
            )
        val smoothMedias =
            listOf(
                Uri.parse("http://www.example.com/video.ism"),
                Uri.parse("http://www.example.com/video.isml"),
            )
        val dashMedias =
            listOf(
                Uri.parse("http://www.example.com/video.mpd"),
                Uri.parse("http://www.example.com/video.mpd?token=1234"),
                Uri.parse("http://www.example.com/video.mpd?token=1234&other=5678"),
            )

        val undefinedMedias =
            listOf(
                Uri.parse("http://www.example.com/video"),
                Uri.parse("http://www.example.com/video?token=1234"),
                Uri.parse("http://www.example.com/video.idontexist"),
            )

        progressiveMedias.forEach { uri ->
            val data = TestFactory.createEventData()
            Util.setEventDataFormatTypeAndUrlBasedOnExtension(data, uri)

            assertThat(data.progUrl)
                .withFailMessage("Failed on URI: $uri. Expected progUrl to be $uri but was ${data.progUrl}")
                .isEqualTo(uri.toString())

            assertThat(data.streamFormat)
                .withFailMessage("Failed on URI: $uri. Expected streamFormat to be 'progressive' but was ${data.streamFormat}")
                .isEqualTo(StreamFormat.PROGRESSIVE.toString().lowercase())
        }

        hlsMedias.forEach { uri ->
            val data = TestFactory.createEventData()
            Util.setEventDataFormatTypeAndUrlBasedOnExtension(data, uri)

            assertThat(data.m3u8Url)
                .withFailMessage("Failed on URI: $uri. Expected m3u8Url to be $uri but was ${data.m3u8Url}")
                .isEqualTo(uri.toString())

            assertThat(data.streamFormat)
                .withFailMessage("Failed on URI: $uri. Expected streamFormat to be 'hls' but was ${data.streamFormat}")
                .isEqualTo(StreamFormat.HLS.toString().lowercase())
        }

        smoothMedias.forEach { uri ->
            val data = TestFactory.createEventData()
            Util.setEventDataFormatTypeAndUrlBasedOnExtension(data, uri)

            assertThat(data.progUrl)
                .withFailMessage("Failed on URI: $uri. Expected progUrl to be $uri but was ${data.progUrl}")
                .isEqualTo(uri.toString())

            assertThat(data.streamFormat)
                .withFailMessage("Failed on URI: $uri. Expected streamFormat to be 'smooth' but was ${data.streamFormat}")
                .isEqualTo(StreamFormat.SMOOTH.toString().lowercase())
        }

        dashMedias.forEach { uri ->
            val data = TestFactory.createEventData()
            Util.setEventDataFormatTypeAndUrlBasedOnExtension(data, uri)

            assertThat(data.mpdUrl)
                .withFailMessage("Failed on URI: $uri. Expected mpdUrl to be $uri but was ${data.mpdUrl}")
                .isEqualTo(uri.toString())

            assertThat(data.streamFormat)
                .withFailMessage("Failed on URI: $uri. Expected streamFormat to be 'dash' but was ${data.streamFormat}")
                .isEqualTo(StreamFormat.DASH.toString().lowercase())
        }

        undefinedMedias.forEach { uri ->
            val data = TestFactory.createEventData()
            Util.setEventDataFormatTypeAndUrlBasedOnExtension(data, uri)

            assertThat(data.progUrl)
                .withFailMessage("Failed on URI: $uri. Expected progUrl to be $uri but was ${data.progUrl}")
                .isEqualTo(uri.toString())

            assertThat(data.streamFormat)
                .withFailMessage("Failed on URI: $uri. Expected streamFormat to be null but was ${data.streamFormat}")
                .isEqualTo(null)
        }
    }

    @Test
    fun testTopOfStacktrace_Should_includeExceptionName() {
        try {
            throw RuntimeException("RUNTIMEEXCEPTION")
        } catch (e: Exception) {
            val top = e.topOfStacktrace
            assertThat(top.size).isGreaterThan(4)
            assertThat(top.size).isLessThanOrEqualTo(50)
            assertThat(top[0]).contains("java.lang.RuntimeException: RUNTIMEEXCEPTION")
            assertThat(top).anySatisfy { element -> assertThat(element).contains("testTopOfStacktrace") }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsConfigValueTrueIfPlayerNotReady() {
        val config = BitmovinAnalyticsConfig()
        config.isLive = true

        val isLive = Util.getIsLiveFromConfigOrPlayer(false, config.isLive, false)
        assertThat(isLive).isTrue
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsConfigValueFalseIfPlayerNotReady() {
        val config = BitmovinAnalyticsConfig()
        config.isLive = false

        val isLive = Util.getIsLiveFromConfigOrPlayer(false, config.isLive, false)
        assertThat(isLive).isFalse
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsPlayerIsLiveTrueIfPlayerReady() {
        val config = BitmovinAnalyticsConfig()
        config.isLive = true

        val isLive = Util.getIsLiveFromConfigOrPlayer(true, config.isLive, true)
        assertThat(isLive).isTrue
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsPlayerIsLiveFalseIfPlayerReady() {
        val config = BitmovinAnalyticsConfig()
        config.isLive = true

        val isLive = Util.getIsLiveFromConfigOrPlayer(true, config.isLive, false)
        assertThat(isLive).isFalse
    }

    @Test
    fun toPrimitiveLong() {
        assertThat(Util.toPrimitiveLong(-1.2)).isEqualTo(-1)
        assertThat(Util.toPrimitiveLong(-1.6)).isEqualTo(-1)
        assertThat(Util.toPrimitiveLong(0.0)).isEqualTo(0)
        assertThat(Util.toPrimitiveLong(1.2)).isEqualTo(1)
        assertThat(Util.toPrimitiveLong(1.6)).isEqualTo(1)
        assertThat(Util.toPrimitiveLong(null)).isEqualTo(0)
    }

    @Test
    fun multiplyInteger() {
        Assert.assertNull(Util.multiply(null, null as? Int?))
        Assert.assertNull(Util.multiply(null, 1 as Int?))
        Assert.assertNull(Util.multiply(1.0, null as? Int?))
        Assert.assertEquals(Util.multiply(1.0, 1 as Int?)!!, 1.0, 0.0)
    }

    @Test
    fun secondsToMillis() {
        Assert.assertEquals(Util.secondsToMillis(null), 0)
        Assert.assertEquals(Util.secondsToMillis(0.0), 0)
        Assert.assertEquals(Util.secondsToMillis(3.5), 3500)
    }

    @Test
    @Config(sdk = [33])
    fun getApplicationInfo_StartingSDK33() {
        // arrange
        val context = RuntimeEnvironment.getApplication()

        // act
        val applicationInfo = Util.getApplicationInfoOrNull(context)

        // assert
        Assert.assertNotNull(applicationInfo)
    }

    @Test
    @Config(sdk = [30])
    fun getApplicationInfo_BeforeSDK33() {
        // arrange
        val context = RuntimeEnvironment.getApplication()

        // act
        val applicationInfo = Util.getApplicationInfoOrNull(context)

        // assert
        Assert.assertNotNull(applicationInfo)
    }

    @Test
    @Config(sdk = [33])
    fun getPackageInfo_StartingSDK33() {
        // arrange
        val context = RuntimeEnvironment.getApplication()

        // act
        val packageInfo = Util.getPackageInfoOrNull(context)

        // assert
        Assert.assertNotNull(packageInfo)
    }

    @Test
    @Config(sdk = [30])
    fun getPackageInfo_BeforeSDK33() {
        // arrange
        val context = RuntimeEnvironment.getApplication()

        // act
        val packageInfo = Util.getPackageInfoOrNull(context)

        // assert
        Assert.assertNotNull(packageInfo)
    }

    @Test
    fun `isLikelyVideoMimeType should return true for mp4 containerMimeType`() {
        assertThat(Util.isLikelyVideoMimeType("video/mp4")).isTrue()
    }

    @Test
    fun `isLikelyVideoMimeType should return true for avc sampleMimeType`() {
        assertThat(Util.isLikelyVideoMimeType("video/avc")).isTrue()
    }

    @Test
    fun `isLikelyVideoMimeType should return false for audio sampleMimeType`() {
        assertThat(Util.isLikelyVideoMimeType("audio/mp4a-latm")).isFalse()
    }

    @Test
    fun `isLikelyVideoMimeType should return false for application mimetype`() {
        assertThat(Util.isLikelyVideoMimeType("application/vnd.3gpp.mcvideo-user-profile+xml")).isFalse()
    }

    @Test
    fun `isLikelyVideoMimeType should return false for audio containerMimeType`() {
        assertThat(Util.isLikelyVideoMimeType("audio/mp4")).isFalse()
    }

    @Test
    fun `isLikelyProgressiveStream should return false for non progressive segment`() {
        assertThat(Util.isLikelyProgressiveStream("http://www.example.com/video.m3u8".toUri())).isFalse()
    }

    @Test
    fun `isLikelyProgressiveStream should return true for progressive segment`() {
        assertThat(Util.isLikelyProgressiveStream("http://www.example.com/video.mp4".toUri())).isTrue()
    }

    @Test
    fun `isLikelyVideoSegment should return true for progressive segment`() {
        assertThat(Util.isLikelyVideoSegment("http://www.example.com/video.mp4".toUri())).isTrue()
    }

    @Test
    fun `isLikelyVideoSegment should return false for nonVideoSegment`() {
        assertThat(Util.isLikelyVideoSegment("http://www.example.com/video.m3u8".toUri())).isFalse()
        assertThat(Util.isLikelyVideoSegment("http://www.example.com/video.mp3".toUri())).isFalse()
        assertThat(Util.isLikelyVideoSegment("http://www.example.com/video.mpd".toUri())).isFalse()
    }
}
