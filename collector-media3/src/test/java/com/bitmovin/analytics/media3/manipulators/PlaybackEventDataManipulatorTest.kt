package com.bitmovin.analytics.media3.manipulators

import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.manifest.DashManifest
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.DownloadSpeedInfo
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.media3.TestUtils
import com.bitmovin.analytics.media3.player.DrmInfoProvider
import com.bitmovin.analytics.media3.player.PlaybackInfoProvider
import com.bitmovin.analytics.media3.player.PlayerStatisticsProvider
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.Collections

class PlaybackEventDataManipulatorTest {
    private lateinit var mockExoPlayer: ExoPlayer
    private lateinit var mockPlaybackInfoProvider: PlaybackInfoProvider
    private lateinit var mockMetadataProvider: MetadataProvider
    private lateinit var mockDrmInfoProvider: DrmInfoProvider
    private lateinit var mockPlayerStatisticsProvider: PlayerStatisticsProvider
    private lateinit var mockDownloadSpeedMeter: DownloadSpeedMeter
    private lateinit var playbackEventDataManipulator: PlaybackEventDataManipulator

    @Before
    fun setup() {
        mockExoPlayer = mockk(relaxed = true)
        mockPlaybackInfoProvider = mockk(relaxed = true)
        mockMetadataProvider = mockk(relaxed = true)
        mockDrmInfoProvider = mockk(relaxed = true)
        mockPlayerStatisticsProvider = mockk(relaxed = true)
        mockDownloadSpeedMeter = mockk(relaxed = true)
        playbackEventDataManipulator = PlaybackEventDataManipulator(mockExoPlayer, mockPlaybackInfoProvider, mockMetadataProvider, mockDrmInfoProvider, mockPlayerStatisticsProvider, mockDownloadSpeedMeter)
    }

    @Test
    fun `playing live stream should set fields correctly`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()

        // prepare isLive
        every { mockPlaybackInfoProvider.playerIsReady } returns true
        every { mockExoPlayer.isCurrentMediaItemDynamic } returns true
        every { mockExoPlayer.duration } returns 1234L

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
        assertThat(eventData.videoDuration).isEqualTo(0)
        assertThat(eventData.ad).isEqualTo(0)
    }

    @Test
    fun `use config on startup to determine if live`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()

        // prepare isLive
        val sourceMetadata = SourceMetadata(isLive = true)
        every { mockMetadataProvider.getSourceMetadata() } returns sourceMetadata
        every { mockPlaybackInfoProvider.playerIsReady } returns false
        every { mockExoPlayer.isCurrentMediaItemDynamic } returns false
        every { mockExoPlayer.duration } answers { 1234L }

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
        assertThat(eventData.videoDuration).isEqualTo(0)
        assertThat(eventData.ad).isEqualTo(0)
    }

    @Test
    fun `track duration when not live`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()
        every { mockPlaybackInfoProvider.playerIsReady } returns true
        every { mockExoPlayer.isCurrentMediaItemDynamic } returns false
        every { mockExoPlayer.duration } answers { 1234L }

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isFalse
        assertThat(eventData.videoDuration).isEqualTo(1234L)
    }

    @Test
    fun `track download speed and dropped frames`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()

        val speedInfo = DownloadSpeedInfo()
        speedInfo.avgDownloadSpeed = 1.0f
        every { mockDownloadSpeedMeter.getInfo() } returns speedInfo
        every { mockPlayerStatisticsProvider.getAndResetDroppedFrames() } returns 456

        // act
        playbackEventDataManipulator.manipulate(eventData)

        assertThat(eventData.downloadSpeedInfo?.avgDownloadSpeed).isEqualTo(1.0f)
        assertThat(eventData.droppedFrames).isEqualTo(456)
    }

    @Test
    fun `track drm scheme`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()
        every { mockDrmInfoProvider.drmType } returns "widevine"

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.drmType).isEqualTo("widevine")
    }

    @Test
    fun `track player is muted when volume is smaller 0_01`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()
        every { mockExoPlayer.isCommandAvailable(Player.COMMAND_GET_VOLUME) } returns true
        every { mockExoPlayer.volume } returns 0.0f

        every { mockExoPlayer.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME) } returns true
        every { mockExoPlayer.deviceVolume } returns 20
        every { mockExoPlayer.isDeviceMuted } returns false

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isMuted).isTrue
    }

    @Test
    fun `track player is muted when device is muted`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()
        every { mockExoPlayer.isCommandAvailable(Player.COMMAND_GET_VOLUME) } returns true
        every { mockExoPlayer.volume } returns 0.4f

        every { mockExoPlayer.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME) } returns true
        every { mockExoPlayer.deviceVolume } returns 20
        every { mockExoPlayer.isDeviceMuted } returns true

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isMuted).isTrue
    }

    @Test
    fun `track player is muted when device volume is 0`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()
        every { mockExoPlayer.isCommandAvailable(Player.COMMAND_GET_VOLUME) } returns true
        every { mockExoPlayer.volume } returns 0.4f

        every { mockExoPlayer.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME) } returns true
        every { mockExoPlayer.deviceVolume } returns 0
        every { mockExoPlayer.isDeviceMuted } returns false

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isMuted).isTrue
    }

    @Test
    fun `track player is not muted`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()

        every { mockExoPlayer.isCommandAvailable(Player.COMMAND_GET_VOLUME) } returns true
        every { mockExoPlayer.volume } returns 0.4f

        every { mockExoPlayer.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME) } returns true
        every { mockExoPlayer.deviceVolume } returns 20
        every { mockExoPlayer.isDeviceMuted } returns false

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isMuted).isFalse
    }

    @Test
    fun `when dash stream is played then dash metadata is set using location from manifest directly`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()
        val testUri = mockk<Uri>(relaxed = true)
        every { testUri.toString() } returns "http://test.com"
        val dashManifest = DashManifest(0, 0, 0, false, 0, 0, 0, 0, null, null, null, testUri, Collections.emptyList())
        every { mockExoPlayer.currentManifest } returns dashManifest

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.streamFormat).isEqualTo("dash")
        assertThat(eventData.mpdUrl).isEqualTo("http://test.com")
    }

    @Test
    fun `when dash stream is played then dash metadata is set using location from playbackprovider`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()
        val dashManifest = DashManifest(0, 0, 0, false, 0, 0, 0, 0, null, null, null, null, Collections.emptyList())
        every { mockExoPlayer.currentManifest } returns dashManifest
        every { mockPlaybackInfoProvider.manifestUrl } returns "http://test3.com"

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.streamFormat).isEqualTo("dash")
        assertThat(eventData.mpdUrl).isEqualTo("http://test3.com")
    }

    @Test
    fun `when ad is playing then ad is set`() {
        // arrange
        val eventData = TestUtils.createMinimalEventData()
        every { mockExoPlayer.isPlayingAd } returns true

        // act
        playbackEventDataManipulator.manipulate(eventData)

        // assert
        assertThat(eventData.ad).isEqualTo(1)
    }
}
