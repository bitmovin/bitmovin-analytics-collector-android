package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.media.audio.quality.AudioQuality
import com.bitmovin.player.api.media.video.quality.VideoQuality
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
        assertThat(qualityProvider.currentVideoQuality).isEqualTo(quality)
    }

    @Test
    fun test_currentVideoQuality_Should_returnQualityStored() {
        // arrange
        val quality = VideoQuality("id", "label", 123, 123, 123, "dummy", 30.0f, 1, 1)
        qualityProvider.currentVideoQuality = quality

        // act and assert
        assertThat(qualityProvider.currentVideoQuality).isEqualTo(quality)
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

        qualityProvider.currentVideoQuality = oldVideoQuality

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

        qualityProvider.currentVideoQuality = oldVideoQuality

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
    fun test_resetPlaybackQualities_Should_setQualitiesToNull() {
        // arrage
        val oldVideoQuality = VideoQuality("id", "label", 123, 123, 123, "dummy", 30.0f, 1, 1)
        val oldAudioQuality = AudioQuality("id", "label", 123, 123, 123, "dummy", 2)

        val newVideoQuality = VideoQuality("id2", "label2", 456, 123, 123, "dummy", 30.0f, 1, 1)
        val newAudioQuality = AudioQuality("id2", "label2", 456, 123, 123, "dummy", 2)

        // since qualities are null after reset, player will be called for current qualities
        every { playerMock.playbackAudioData }.returns(newAudioQuality)
        every { playerMock.playbackVideoData }.returns(newVideoQuality)

        qualityProvider.currentVideoQuality = oldVideoQuality
        qualityProvider.currentAudioQuality = oldAudioQuality

        // act
        qualityProvider.resetPlaybackQualities()

        // assert
        assertThat(qualityProvider.currentVideoQuality).isEqualTo(newVideoQuality)
        assertThat(qualityProvider.currentAudioQuality).isEqualTo(newAudioQuality)
    }
}
