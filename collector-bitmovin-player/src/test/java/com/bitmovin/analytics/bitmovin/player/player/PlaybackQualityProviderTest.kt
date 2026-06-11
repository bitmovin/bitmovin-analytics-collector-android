package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.media.audio.quality.AudioQuality
import com.bitmovin.player.api.media.video.quality.VideoQuality
import com.bitmovin.player.api.source.Source
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class PlaybackQualityProviderTest {
    private lateinit var playerMock: Player
    private lateinit var qualityProvider: PlaybackQualityProvider

    @Before
    fun setup() {
        playerMock = mockk(relaxed = true)
        qualityProvider = PlaybackQualityProvider(playerMock)
    }

    @Test
    fun test_currentVideoQuality_Should_returnQualityFromPlayerInitially() {
        // arrange
        val quality = VideoQuality("id", "label", 123, 123, 123, "dummy", 30.0f, 1, 1)
        every { playerMock.playbackVideoData }.returns(quality)

        // act and assert
        assertThat(qualityProvider.getVideoQualityHolder()?.currentVideoQuality).isEqualTo(quality)
    }

    @Test
    fun test_currentVideoQuality_Should_returnQualityStored() {
        // arrange
        val quality = VideoQuality("id", "label", 123, 123, 123, "dummy", 30.0f, 1, 1)
        qualityProvider.setVideoQuality(quality)

        // act and assert
        assertThat(qualityProvider.getVideoQualityHolder()?.currentVideoQuality).isEqualTo(quality)
    }

    @Test
    fun test_currentAudioQuality_Should_returnQualityStored() {
        // arrange
        val quality = AudioQuality("id", "label", 123, 123, 123, "dummy", 2)
        qualityProvider.currentAudioQuality = quality

        // act and assert
        assertThat(qualityProvider.currentAudioQuality).isEqualTo(quality)
    }

    @Test
    fun test_currentAudioQuality_Should_returnQualityFromPlayerInitially() {
        // arrange
        val quality = AudioQuality("id", "label", 123, 123, 123, "dummy", 2)
        every { playerMock.playbackAudioData }.returns(quality)

        // act and assert
        assertThat(qualityProvider.currentAudioQuality).isEqualTo(quality)
    }

    @Test
    fun test_didVideoQualityChange_Should_returnFalseWithSameQuality() {
        // arrage
        val oldVideoQuality = VideoQuality("id", "label", 123, 123, 123, "dummy", 30.0f, 1, 1)
        val newVideoQuality = VideoQuality("id", "label", 123, 123, 123, "dummy", 30.0f, 1, 1)

        qualityProvider.setVideoQuality(oldVideoQuality)

        // act
        val didChange = qualityProvider.didVideoQualityChange(newVideoQuality)

        // assert
        assertThat(didChange).isFalse
    }

    @Test
    fun test_didVideoQualityChange_Should_returnTrueWithDifferentQuality() {
        // arrage
        val oldVideoQuality = VideoQuality("id", "label", 123, 123, 123, "dummy", 30.0f, 1, 1)
        val newVideoQuality = VideoQuality("id", "label", 456, 123, 123, "dummy", 30.0f, 1, 1)

        qualityProvider.setVideoQuality(oldVideoQuality)

        // act
        val didChange = qualityProvider.didVideoQualityChange(newVideoQuality)

        // assert
        assertThat(didChange).isTrue
    }

    @Test
    fun test_didAudioQualityChange_Should_returnFalseWithSameQuality() {
        // arrage
        val oldAudioQuality = AudioQuality("id", "label", 123, 123, 123, "dummy", 2)
        val newAudioQuality = AudioQuality("id", "label", 123, 123, 123, "dummy", 2)

        qualityProvider.currentAudioQuality = oldAudioQuality

        // act
        val didChange = qualityProvider.didAudioQualityChange(newAudioQuality)

        // assert
        assertThat(didChange).isFalse
    }

    @Test
    fun test_didAudioQualityChange_Should_returnTrueWithDifferentQuality() {
        // arrage
        val oldAudioQuality = AudioQuality("id", "label", 123, 123, 123, "dummy", 2)
        val newAudioQuality = AudioQuality("id2", "label2", 456, 123, 123, "dummy", 2)

        qualityProvider.currentAudioQuality = oldAudioQuality

        // act
        val didChange = qualityProvider.didAudioQualityChange(newAudioQuality)

        // assert
        assertThat(didChange).isTrue
    }

    @Test
    fun test_currentVideoManifestBitrate_Should_resolveManifestBitrateOfMatchingAvailableQuality() {
        // arrange
        // the playing quality reports the actual media bitrate (14097792), which differs from the
        // bitrate the same representation (matched by id) declares in the manifest (4800000)
        val playingQuality = VideoQuality("1080_4800000", "label", 14097792, -1, 14097792, "avc1.4D4032", 25.0f, 1920, 1080)
        val manifestQuality = VideoQuality("1080_4800000", "label", 4800000, -1, 4800000, "avc1.42c00d", 25.0f, 1920, 1080)
        val sourceMock = mockk<Source>(relaxed = true)
        every { sourceMock.availableVideoQualities }.returns(listOf(manifestQuality))
        every { playerMock.source }.returns(sourceMock)

        // act
        qualityProvider.setVideoQuality(playingQuality)

        // assert
        assertThat(qualityProvider.getVideoQualityHolder()?.currentBitrateFromManifest).isEqualTo(4800000)
    }

    @Test
    fun test_currentVideoManifestBitrate_Should_fallBackToQualityBitrateWhenNoManifestMatch() {
        // arrange
        val playingQuality = VideoQuality("only_in_playback", "label", 14097792, -1, 14097792, "avc1.4D4032", 25.0f, 1920, 1080)
        val sourceMock = mockk<Source>(relaxed = true)
        every { sourceMock.availableVideoQualities }.returns(emptyList())
        every { playerMock.source }.returns(sourceMock)

        // act
        qualityProvider.setVideoQuality(playingQuality)

        // assert
        assertThat(qualityProvider.getVideoQualityHolder()?.currentBitrateFromManifest).isEqualTo(14097792)
    }

    @Test
    fun test_currentVideoManifestBitrate_Should_fallBackToQualityBitrateWhenNoSource() {
        // arrange
        val playingQuality = VideoQuality("id", "label", 628000, -1, 628000, "avc1.4D400D", 25.0f, 640, 360)
        every { playerMock.source }.returns(null)

        // act
        qualityProvider.setVideoQuality(playingQuality)

        // assert
        assertThat(qualityProvider.getVideoQualityHolder()?.currentBitrateFromManifest).isEqualTo(628000)
    }

    @Test
    fun test_currentVideoManifestBitrate_Should_beResolvedAtCaptureTimeNotReResolvedOnAccess() {
        // arrange: capture the quality while the DASH source is active (id matches -> 4800000)
        val playingQuality = VideoQuality("1080_4800000", "label", 14097792, -1, 14097792, "avc1.4D4032", 25.0f, 1920, 1080)
        val dashSource = mockk<Source>(relaxed = true)
        every { dashSource.availableVideoQualities }.returns(
            listOf(VideoQuality("1080_4800000", "label", 4800000, -1, 4800000, "avc1.42c00d", 25.0f, 1920, 1080)),
        )
        every { playerMock.source }.returns(dashSource)
        qualityProvider.setVideoQuality(playingQuality)

        // act: the active source advances to a different source (playlist) where the id no longer matches
        val nextSource = mockk<Source>(relaxed = true)
        every { nextSource.availableVideoQualities }.returns(
            listOf(VideoQuality("1", "label", 9979760, -1, 9979760, "avc1", 25.0f, 1920, 1080)),
        )
        every { playerMock.source }.returns(nextSource)

        // assert: the manifest bitrate stays the value resolved at capture time, not re-resolved
        assertThat(qualityProvider.getVideoQualityHolder()?.currentBitrateFromManifest).isEqualTo(4800000)
    }

    @Test
    fun test_resetPlaybackQualities_Should_setQualitiesToNull() {
        // arrage
        val oldVideoQuality = VideoQuality("id", "label", 123, 123, 123, "dummy", 30.0f, 1, 1)
        val oldAudioQuality = AudioQuality("id", "label", 123, 123, 123, "dummy", 2)

        val newVideoQuality = VideoQuality("id2", "label2", 456, 123, 123, "dummy", 30.0f, 1, 1)
        val newAudioQuality = AudioQuality("id2", "label2", 456, 123, 123, "dummy", 2)

        // since qualities are null after reset, player will be called for current qualities
        every { playerMock.playbackAudioData }.returns(newAudioQuality)
        every { playerMock.playbackVideoData }.returns(newVideoQuality)

        qualityProvider.setVideoQuality(oldVideoQuality)
        qualityProvider.currentAudioQuality = oldAudioQuality

        // act
        qualityProvider.resetPlaybackQualities()

        // assert
        assertThat(qualityProvider.getVideoQualityHolder()?.currentVideoQuality).isEqualTo(newVideoQuality)
        assertThat(qualityProvider.currentAudioQuality).isEqualTo(newAudioQuality)
    }
}
