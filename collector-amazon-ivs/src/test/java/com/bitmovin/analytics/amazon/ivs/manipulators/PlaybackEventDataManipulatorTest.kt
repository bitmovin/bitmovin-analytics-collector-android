package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.amazon.ivs.TestUtils
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PlaybackEventDataManipulatorTest {

    @Test
    fun `manipulate should not set videoId if it is already set on config`() {
        // arrange
        val mockedConfig = mockk<BitmovinAnalyticsConfig>(relaxed = true)
        every { mockedConfig.videoId } returns "test-video-id"

        val manipulator = createPlaybackEventDataManipulator(config = mockedConfig)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoId).isNull()
    }

    @Test
    fun `manipulate should not set videoId if sessionId or ChannelId is invalid`() {
        // arrange test table
        val nullUrlConfig = mockk<BitmovinAnalyticsConfig>(relaxed = true)
        every { nullUrlConfig.m3u8Url } returns null
        every { nullUrlConfig.videoId } returns null

        val anyUrlConfig = mockk<BitmovinAnalyticsConfig>(relaxed = true)
        every { anyUrlConfig.m3u8Url } returns "http://localhost:1234/test.m3u8"
        every { anyUrlConfig.videoId } returns null

        val invalidIvsUrlConfig = mockk<BitmovinAnalyticsConfig>(relaxed = true)
        every { invalidIvsUrlConfig.m3u8Url } returns "http://localhost:1234/.channel..m3u8"
        every { invalidIvsUrlConfig.videoId } returns null

        val correctUrlConfig = mockk<BitmovinAnalyticsConfig>(relaxed = true)
        every { correctUrlConfig.m3u8Url } returns "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.DmumNckWFTqz.m3u8"
        every { correctUrlConfig.videoId } returns null

        val invalidSessionIdPlayer = mockk<Player>(relaxed = true)
        every { invalidSessionIdPlayer.sessionId } returns ""

        val validSessionIdPlayer = mockk<Player>(relaxed = true)
        every { validSessionIdPlayer.sessionId } returns "session_id"

        val testTable = listOf(
            invalidSessionIdPlayer to nullUrlConfig,
            validSessionIdPlayer to nullUrlConfig,
            validSessionIdPlayer to anyUrlConfig,
            validSessionIdPlayer to invalidIvsUrlConfig,
            invalidSessionIdPlayer to correctUrlConfig,
        )

        testTable.forEach {
            // arrange
            val config = it.second
            val player = it.first
            val manipulator = createPlaybackEventDataManipulator(player, config)
            val eventData = TestUtils.createMinimalEventData()

            // act
            manipulator.manipulate(eventData)

            // assert
            assertThat(eventData.videoId).isNull()
        }
    }

    @Test
    fun `manipulate should set videoId automatically`() {
        // arrange
        val sessionId = "session_id"
        val channelId = "channel_id"

        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.sessionId } returns sessionId

        val mockedConfig = mockk<BitmovinAnalyticsConfig>(relaxed = true)
        every { mockedConfig.m3u8Url } returns "https://fcc3ddae59ed.us-west-2.playback.live-video.net/api/video/v1/us-west-2.893648527354.channel.$channelId.m3u8"
        every { mockedConfig.videoId } returns null

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer, mockedConfig)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.videoId).isEqualTo("$channelId-$sessionId")
    }

    @Test
    fun `manipulate should set is live from config`() {
        // arrange
        val mockedConfig = mockk<BitmovinAnalyticsConfig>(relaxed = true)
        every { mockedConfig.isLive } returns true

        val manipulator = createPlaybackEventDataManipulator(config = mockedConfig)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
    }

    @Test
    fun `manipulate should set is live from player`() {
        // arrange
        val mockedPlayer = mockk<Player>(relaxed = true)
        every { mockedPlayer.duration } returns -1L

        val mockedConfig = mockk<BitmovinAnalyticsConfig>(relaxed = true)
        every { mockedConfig.isLive } returns null

        val manipulator = createPlaybackEventDataManipulator(mockedPlayer, mockedConfig)
        val eventData = TestUtils.createMinimalEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
    }

    private fun createPlaybackEventDataManipulator(
        player: Player = mockk(relaxed = true),
        config: BitmovinAnalyticsConfig = mockk(relaxed = true),
    ): PlaybackEventDataManipulator {
        return PlaybackEventDataManipulator(player, config)
    }
}
