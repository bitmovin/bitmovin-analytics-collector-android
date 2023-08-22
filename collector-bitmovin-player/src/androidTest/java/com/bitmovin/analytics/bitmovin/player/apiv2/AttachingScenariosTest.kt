package com.bitmovin.analytics.bitmovin.player.apiv2

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.bitmovin.player.BitmovinPlaybackUtils
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.playlist.PlaylistConfig
import com.bitmovin.player.api.playlist.PlaylistOptions
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AttachingScenariosTest {

    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private var defaultSample1 = TestSources.HLS_REDBULL
    private var defaultSource1 = Source.create(SourceConfig.fromUrl(defaultSample1.m3u8Url!!))

    private var defaultSample2 = TestSources.DASH
    private var defaultSource2 = Source.create(SourceConfig.fromUrl(defaultSample2.mpdUrl!!))

    private lateinit var defaultAnalyticsConfig: BitmovinAnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        // purging database to have a clean state for each test
        EventDatabaseTestHelper.purge(appContext)

        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createBitmovinAnalyticsConfig(backendUrl = mockedIngressUrl)
    }

    @Test
    fun test_vod2ItemsPlaylist_attachingOnPlaylistTransitionEventWithSlowSeekAndAutoplay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig()
        playbackConfig.isAutoplayEnabled = true
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)
        val playlistConfig = PlaylistConfig(listOf(defaultSource1, defaultSource2), PlaylistOptions())

        // act
        attachOnPlaylistTransitionSlowSeek(localPlayer, collector, playlistConfig)

        // assert
        verifyExactlyOneSessionWithStartupSample()
    }

    @Test
    @Ignore("Attaching on playlist transition event is not supported!")
    fun test_vod2ItemsPlaylist_attachingOnPlaylistTransitionEventWithSlowSeekAndWithoutAutoplay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig()
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)
        val playlistConfig = PlaylistConfig(listOf(defaultSource1, defaultSource2), PlaylistOptions())

        // act
        attachOnPlaylistTransitionSlowSeek(localPlayer, collector, playlistConfig)

        // assert
        verifyExactlyOneSessionWithStartupSample()
    }

    @Test
    fun test_vod2ItemsPlaylist_attachOnPlaylistTransitionEventWithFastSeekAndAutoplay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig()
        playbackConfig.isAutoplayEnabled = true
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)
        val playlistConfig = PlaylistConfig(listOf(defaultSource1, defaultSource2), PlaylistOptions())

        // act
        attachOnPlaylistTransitionFastSeek(localPlayer, collector, playlistConfig)

        // assert
        verifyExactlyOneSessionWithStartupSample()
    }

    @Test
    @Ignore("Attaching on playlist transition event is not supported!")
    fun test_vod2ItemsPlaylist_attachOnPlaylistTransitionEventWithFastSeekWithoutAutoplay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig()
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)
        val playlistConfig = PlaylistConfig(listOf(defaultSource1, defaultSource2), PlaylistOptions())

        // act
        attachOnPlaylistTransitionFastSeek(localPlayer, collector, playlistConfig)

        // assert
        verifyExactlyOneSessionWithStartupSample()
    }

    @Test
    fun test_vod2ItemsPlaylist_attachImmediatelyAfterFastSeekToSecondItemWithAutoplay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig()
        playbackConfig.isAutoplayEnabled = true
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)
        val playlistConfig = PlaylistConfig(listOf(defaultSource1, defaultSource2), PlaylistOptions())

        // act
        attachImmediatelyAfterFastSeek(localPlayer, collector, playlistConfig)

        // assert
        verifyExactlyOneSessionWithStartupSample()
    }

    @Test
    fun test_vod2ItemsPlaylist_attachImmediatelyAfterFastSeekToSecondItemWithoutAutoplay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig()
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)
        val playlistConfig = PlaylistConfig(listOf(defaultSource1, defaultSource2), PlaylistOptions())

        // act
        attachImmediatelyAfterFastSeek(localPlayer, collector, playlistConfig)

        // assert
        verifyExactlyOneSessionWithStartupSample()
    }

    @Test
    @Ignore("Late attaching not supported")
    fun test_vod_lateAttachingWhilePlayingWithoutAutoplay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig()
        playbackConfig.isAutoplayEnabled = false
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)
        val playlistConfig = PlaylistConfig(listOf(defaultSource1), PlaylistOptions())

        // act
        lateAttachingWhilePlaying(localPlayer, collector, playlistConfig)
        // assert
        verifyExactlyOneSessionWithStartupSample()
    }

    @Test
    fun test_vod_lateAttachingWhilePlayingWithAutoplay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig()
        playbackConfig.isAutoplayEnabled = true
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)
        val playlistConfig = PlaylistConfig(listOf(defaultSource1), PlaylistOptions())

        // act
        lateAttachingWhilePlaying(localPlayer, collector, playlistConfig)

        // assert
        verifyExactlyOneSessionWithStartupSample()
    }

    // TODO [AN-3602]: this test reveals a bug since our current implementation might overreport sessions if customer
    // attaches a couple of seconds before the video is loaded and is using autoplay on the current source)
    @Test
    @Ignore("reveals a bug in our current implementation")
    fun test_vod2Impressions_attachingBeforeLoadOfSecondImpressionWithAutoplay() {
        // arrange
        val collector = IBitmovinPlayerCollector.create(defaultAnalyticsConfig, appContext)
        val playbackConfig = PlaybackConfig()
        playbackConfig.isAutoplayEnabled = true
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig)
        val localPlayer = Player.create(appContext, playerConfig)

        // act
        mainScope.launch {
            collector.attachPlayer(localPlayer)
            localPlayer.load(defaultSource1)
            localPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 1000)

        mainScope.launch {
            collector.detachPlayer()
            collector.attachPlayer(localPlayer)
        }

        Thread.sleep(2000)

        mainScope.launch {
            localPlayer.load(defaultSource2)
        }

        Thread.sleep(1000)

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

        mainScope.launch {
            localPlayer.destroy()
            collector.detachPlayer()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        Assertions.assertThat(impressionList.size).isEqualTo(2)
    }

    private fun lateAttachingWhilePlaying(player: Player, collector: IBitmovinPlayerCollector, playlistConfig: PlaylistConfig) {
        // act
        mainScope.launch {
            player.load(playlistConfig)
            player.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 1000)

        mainScope.launch {
            collector.attachPlayer(player)
        }

        // wait a bit to make sure late attaching collects data correctly
        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 3000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.destroy()
        }

        Thread.sleep(500)
    }

    private fun attachOnPlaylistTransitionFastSeek(player: Player, collector: IBitmovinPlayerCollector, playlistConfig: PlaylistConfig) {
        // act
        mainScope.launch {
            player.load(playlistConfig)
            player.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 1000)

        // attach automatically on playlist transition
        player.on(PlayerEvent.PlaylistTransition::class.java) {
            mainScope.launch {
                collector.attachPlayer(player)
            }
        }

        mainScope.launch {
            // fast seek to next item
            player.playlist.seek(defaultSource2, 0.0)
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.destroy()
        }

        Thread.sleep(500)
    }

    private fun attachImmediatelyAfterFastSeek(player: Player, collector: IBitmovinPlayerCollector, playlistConfig: PlaylistConfig) {
        mainScope.launch {
            player.load(playlistConfig)
            player.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 1000)

        mainScope.launch {
            // seek to next item
            player.playlist.seek(defaultSource2, 0.0)
            collector.attachPlayer(player)
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.destroy()
        }

        Thread.sleep(500)
    }

    private fun attachOnPlaylistTransitionSlowSeek(player: Player, collector: IBitmovinPlayerCollector, playlistConfig: PlaylistConfig) {
        // act
        mainScope.launch {
            player.load(playlistConfig)
            player.play()
        }

        // attach automatically on playlist transition
        player.on(PlayerEvent.PlaylistTransition::class.java) {
            mainScope.launch {
                collector.attachPlayer(player)
            }
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player, 1000)

        mainScope.launch {
            // seek to end of first item and wait for second item to start playing (slow seek)
            player.duration.let { player.seek(it - 4.00) }
        }

        BitmovinPlaybackUtils.waitUntilNextSourcePlayedToMs(player, 100)

        // wait a bit to make sure late attaching collects data correctly
        Thread.sleep(1000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.destroy()
        }

        Thread.sleep(500)
    }

    private fun verifyExactlyOneSessionWithStartupSample() {
        val impressionList = MockedIngress.extractImpressions()
        Assertions.assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        Assertions.assertThat(eventDataList).hasSizeGreaterThanOrEqualTo(2)

        // make sure we have a startup sample and non 0 videoStartuptime also for late attaching
        eventDataList[0].state = "startup"
        eventDataList[0].videoStartupTime > 0
        eventDataList[1].state = "playing"

        DataVerifier.verifyInvariants(eventDataList)

        val impressionId = eventDataList[0].impressionId
        Assertions.assertThat(eventDataList).allMatch { it.impressionId == impressionId }
    }
}
