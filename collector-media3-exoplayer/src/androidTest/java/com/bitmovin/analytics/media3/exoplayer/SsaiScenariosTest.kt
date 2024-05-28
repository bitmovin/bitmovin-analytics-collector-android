package com.bitmovin.analytics.media3.exoplayer

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdPosition
import com.bitmovin.analytics.media3.exoplayer.api.IMedia3ExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using media3-exoplayer
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
@RunWith(AndroidJUnit4::class)
class SsaiScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val defaultSample = TestSources.HLS_REDBULL
    private val defaultMediaItem = MediaItem.fromUri(defaultSample.m3u8Url!!)
    private val defaultSourceMetadata =
        SourceMetadata(
            customData = CustomData(customData1 = "custom-data-1"),
        )

    private lateinit var player: ExoPlayer
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
        player = ExoPlayer.Builder(appContext).build()
    }

    @After
    fun teardown() {
        MockedIngress.stopServer()
        mainScope.launch {
            player.release()
        }
        // wait a bit for player to be destroyed
        Thread.sleep(100)
    }

    @Test
    fun test_adBreakStart_adStart_adStart_adBreakEnd_sets_right_values() {
        // arrange
        val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            collector.ssai.adBreakStart(
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
            )
            collector.ssai.adStart(
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
            )
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

        mainScope.launch {
            collector.ssai.adStart(
                SsaiAdMetadata("test-ad-id-2", "test-ad-system-2", CustomData(customData2 = "ad-test-custom-data-2")),
            )
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

        mainScope.launch {
            collector.ssai.adBreakEnd()
            player.pause()
            collector.detachPlayer()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        assertThat(eventDataList.size).isEqualTo(4)
        val startupSample = eventDataList[0]
        assertThat(startupSample.state).isEqualTo("startup")
        assertThat(startupSample.startupTime).isGreaterThan(0)
        assertThat(startupSample.ad).isEqualTo(2)
        assertThat(startupSample.adIndex).isEqualTo(0)
        assertThat(startupSample.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(startupSample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(startupSample.adSystem).isEqualTo("test-ad-system-1")
        assertThat(startupSample.adId).isEqualTo("test-ad-id-1")

        val playingAd1Sample = eventDataList[1]
        assertThat(playingAd1Sample.state).isEqualTo("playing")
        assertThat(playingAd1Sample.ad).isEqualTo(2)
        assertThat(playingAd1Sample.adIndex).isNull()
        assertThat(playingAd1Sample.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(playingAd1Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(playingAd1Sample.adSystem).isEqualTo("test-ad-system-1")
        assertThat(playingAd1Sample.adId).isEqualTo("test-ad-id-1")

        val playingAd2Sample = eventDataList[2]
        assertThat(playingAd2Sample.state).isEqualTo("playing")
        assertThat(playingAd2Sample.ad).isEqualTo(2)
        assertThat(playingAd2Sample.adIndex).isEqualTo(1)
        assertThat(playingAd2Sample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingAd2Sample.customData2).isEqualTo("ad-test-custom-data-2")
        assertThat(playingAd2Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(playingAd2Sample.adSystem).isEqualTo("test-ad-system-2")
        assertThat(playingAd2Sample.adId).isEqualTo("test-ad-id-2")

        val regularPlayingSample = eventDataList[3]
        assertThat(regularPlayingSample.state).isEqualTo("playing")
        assertThat(regularPlayingSample.ad).isEqualTo(0)
        assertThat(regularPlayingSample.adIndex).isNull()
        assertThat(regularPlayingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(regularPlayingSample.customData2).isNull()
        assertThat(regularPlayingSample.adPosition).isNull()
        assertThat(regularPlayingSample.adSystem).isNull()
        assertThat(regularPlayingSample.adId).isNull()
    }

    @Test
    fun test_ignore_adStart_call_if_adBreakStart_has_not_been_called() {
        // arrange
        val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

        mainScope.launch {
            collector.ssai.adStart(SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"))
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
            player.release()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        assertThat(eventDataList.size).isEqualTo(2)
        val startupSample = eventDataList[0]
        assertThat(startupSample.state).isEqualTo("startup")
        assertThat(startupSample.startupTime).isGreaterThan(0)
        assertThat(startupSample.ad).isEqualTo(0)
        assertThat(startupSample.adIndex).isNull()
        assertThat(startupSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(startupSample.adPosition).isNull()
        assertThat(startupSample.adSystem).isNull()
        assertThat(startupSample.adId).isNull()

        val playingSample = eventDataList[1]
        assertThat(playingSample.state).isEqualTo("playing")
        assertThat(playingSample.ad).isEqualTo(0)
        assertThat(playingSample.adIndex).isNull()
        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingSample.adPosition).isNull()
        assertThat(playingSample.adSystem).isNull()
        assertThat(playingSample.adId).isNull()
    }

    @Test
    fun test_ignore_adBreakEnd_call_if_adBreakStart_has_not_been_called() {
        // arrange
        val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

        mainScope.launch {
            collector.ssai.adBreakEnd()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        assertThat(eventDataList.size).isEqualTo(2)
        val startupSample = eventDataList[0]
        assertThat(startupSample.state).isEqualTo("startup")
        assertThat(startupSample.startupTime).isGreaterThan(0)
        assertThat(startupSample.ad).isEqualTo(0)
        assertThat(startupSample.adIndex).isNull()
        assertThat(startupSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(startupSample.adPosition).isNull()
        assertThat(startupSample.adSystem).isNull()
        assertThat(startupSample.adId).isNull()

        val playingSample = eventDataList[1]
        assertThat(playingSample.state).isEqualTo("playing")
        assertThat(playingSample.ad).isEqualTo(0)
        assertThat(playingSample.adIndex).isNull()
        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingSample.adPosition).isNull()
        assertThat(playingSample.adSystem).isNull()
        assertThat(playingSample.adId).isNull()
    }

    @Test
    fun test_no_sample_sent_when_adBreak_was_closed_without_adStart_call_during_adBreak() {
        // arrange
        val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            player.play()
            collector.ssai.adBreakStart(SsaiAdBreakMetadata(SsaiAdPosition.PREROLL))
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

        mainScope.launch {
            collector.ssai.adBreakEnd()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        assertThat(eventDataList.size).isEqualTo(2)
        val startupSample = eventDataList[0]
        assertThat(startupSample.state).isEqualTo("startup")
        assertThat(startupSample.startupTime).isGreaterThan(0)
        assertThat(startupSample.ad).isEqualTo(0)
        assertThat(startupSample.adIndex).isNull()
        assertThat(startupSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(startupSample.adPosition).isNull()
        assertThat(startupSample.adSystem).isNull()
        assertThat(startupSample.adId).isNull()

        val playingSample = eventDataList[1]
        assertThat(playingSample.state).isEqualTo("playing")
        assertThat(playingSample.ad).isEqualTo(0)
        assertThat(playingSample.adIndex).isNull()
        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingSample.adPosition).isNull()
        assertThat(playingSample.adSystem).isNull()
        assertThat(playingSample.adId).isNull()
    }

    @Test
    fun test_do_not_reset_adIndex_between_adBreaks() {
        // arrange
        val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            collector.ssai.adBreakStart(
                SsaiAdBreakMetadata(
                    SsaiAdPosition.PREROLL,
                ),
            )
            collector.ssai.adStart(
                SsaiAdMetadata(
                    "test-ad-id-1",
                    "test-ad-system-1",
                    CustomData(customData1 = "ad-test-custom-data-1"),
                ),
            )
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

        mainScope.launch {
            collector.ssai.adBreakEnd()
            player.pause()
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

        mainScope.launch {
            collector.ssai.adBreakStart(
                SsaiAdBreakMetadata(
                    SsaiAdPosition.MIDROLL,
                ),
            )
            collector.ssai.adStart(SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"))
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        assertThat(eventDataList.size).isEqualTo(6)
        val startupAdSample = eventDataList[0]
        assertThat(startupAdSample.state).isEqualTo("startup")
        assertThat(startupAdSample.startupTime).isGreaterThan(0)
        assertThat(startupAdSample.ad).isEqualTo(2)
        assertThat(startupAdSample.adIndex).isEqualTo(0)
        assertThat(startupAdSample.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(startupAdSample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(startupAdSample.adSystem).isEqualTo("test-ad-system-1")
        assertThat(startupAdSample.adId).isEqualTo("test-ad-id-1")

        val playingAd1Sample = eventDataList[1]
        assertThat(playingAd1Sample.state).isEqualTo("playing")
        assertThat(playingAd1Sample.ad).isEqualTo(2)
        assertThat(playingAd1Sample.adIndex).isNull()
        assertThat(playingAd1Sample.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(playingAd1Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(playingAd1Sample.adSystem).isEqualTo("test-ad-system-1")
        assertThat(playingAd1Sample.adId).isEqualTo("test-ad-id-1")

        val playingSample = eventDataList[2]
        assertThat(playingSample.state).isEqualTo("playing")
        assertThat(playingSample.ad).isEqualTo(0)
        assertThat(playingSample.adIndex).isNull()
        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingSample.adPosition).isNull()
        assertThat(playingSample.adSystem).isNull()
        assertThat(playingSample.adId).isNull()

        val pausedSample = eventDataList[3]
        assertThat(pausedSample.state).isEqualTo("pause")
        assertThat(pausedSample.ad).isEqualTo(0)
        assertThat(pausedSample.adIndex).isNull()
        assertThat(pausedSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(pausedSample.adPosition).isNull()
        assertThat(pausedSample.adSystem).isNull()
        assertThat(pausedSample.adId).isNull()

        val playingSample2 = eventDataList[4]
        assertThat(playingSample2.state).isEqualTo("playing")
        assertThat(playingSample2.ad).isEqualTo(0)
        assertThat(playingSample2.adIndex).isNull()
        assertThat(playingSample2.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingSample2.adPosition).isNull()
        assertThat(playingSample2.adSystem).isNull()
        assertThat(playingSample2.adId).isNull()

        val playingAd2Sample2 = eventDataList[5]
        assertThat(playingAd2Sample2.state).isEqualTo("playing")
        assertThat(playingAd2Sample2.ad).isEqualTo(2)
        assertThat(playingAd2Sample2.adIndex).isEqualTo(1)
        assertThat(playingAd2Sample2.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingAd2Sample2.adPosition).isEqualTo(SsaiAdPosition.MIDROLL.toString())
        assertThat(playingAd2Sample2.adSystem).isEqualTo("test-ad-system-2")
        assertThat(playingAd2Sample2.adId).isEqualTo("test-ad-id-2")
    }

    @Test
    fun test_increase_and_set_adIndex_only_on_every_first_ad_sample() {
        // arrange
        val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            collector.ssai.adBreakStart(
                SsaiAdBreakMetadata(
                    SsaiAdPosition.PREROLL,
                ),
            )
            collector.ssai.adStart(
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
            )
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

        mainScope.launch {
            player.pause()
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

        mainScope.launch {
            collector.ssai.adStart(
                SsaiAdMetadata("test-ad-id-2", "test-ad-system-2", CustomData(customData2 = "ad-test-custom-data-2")),
            )
            player.pause()
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

        mainScope.launch {
            collector.ssai.adBreakEnd()
            player.pause()
            collector.detachPlayer()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        assertThat(eventDataList.size).isEqualTo(8)
        val startupSample = eventDataList[0]
        assertThat(startupSample.state).isEqualTo("startup")
        assertThat(startupSample.startupTime).isGreaterThan(0)
        assertThat(startupSample.ad).isEqualTo(2)
        assertThat(startupSample.adIndex).isEqualTo(0)
        assertThat(startupSample.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(startupSample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(startupSample.adSystem).isEqualTo("test-ad-system-1")
        assertThat(startupSample.adId).isEqualTo("test-ad-id-1")

        val playingAd1Sample = eventDataList[1]
        assertThat(playingAd1Sample.state).isEqualTo("playing")
        assertThat(playingAd1Sample.ad).isEqualTo(2)
        assertThat(playingAd1Sample.adIndex).isNull()
        assertThat(playingAd1Sample.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(playingAd1Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(playingAd1Sample.adSystem).isEqualTo("test-ad-system-1")
        assertThat(playingAd1Sample.adId).isEqualTo("test-ad-id-1")

        val pausedAd1Sample = eventDataList[2]
        assertThat(pausedAd1Sample.state).isEqualTo("pause")
        assertThat(pausedAd1Sample.ad).isEqualTo(2)
        assertThat(pausedAd1Sample.adIndex).isNull()
        assertThat(pausedAd1Sample.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(pausedAd1Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(pausedAd1Sample.adSystem).isEqualTo("test-ad-system-1")
        assertThat(pausedAd1Sample.adId).isEqualTo("test-ad-id-1")

        val playingAd1Sample2 = eventDataList[3]
        assertThat(playingAd1Sample2.state).isEqualTo("playing")
        assertThat(playingAd1Sample2.ad).isEqualTo(2)
        assertThat(playingAd1Sample2.adIndex).isNull()
        assertThat(playingAd1Sample2.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(playingAd1Sample2.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(playingAd1Sample2.adSystem).isEqualTo("test-ad-system-1")
        assertThat(playingAd1Sample2.adId).isEqualTo("test-ad-id-1")

        val playingAd2Sample = eventDataList[4]
        assertThat(playingAd2Sample.state).isEqualTo("playing")
        assertThat(playingAd2Sample.ad).isEqualTo(2)
        assertThat(playingAd2Sample.adIndex).isEqualTo(1)
        assertThat(playingAd2Sample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingAd2Sample.customData2).isEqualTo("ad-test-custom-data-2")
        assertThat(playingAd2Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(playingAd2Sample.adSystem).isEqualTo("test-ad-system-2")
        assertThat(playingAd2Sample.adId).isEqualTo("test-ad-id-2")

        val pausedAd2Sample = eventDataList[5]
        assertThat(pausedAd2Sample.state).isEqualTo("pause")
        assertThat(pausedAd2Sample.ad).isEqualTo(2)
        assertThat(pausedAd2Sample.adIndex).isNull()
        assertThat(pausedAd2Sample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(pausedAd2Sample.customData2).isEqualTo("ad-test-custom-data-2")
        assertThat(pausedAd2Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(pausedAd2Sample.adSystem).isEqualTo("test-ad-system-2")
        assertThat(pausedAd2Sample.adId).isEqualTo("test-ad-id-2")

        val playingAd2Sample2 = eventDataList[6]
        assertThat(playingAd2Sample2.state).isEqualTo("playing")
        assertThat(playingAd2Sample2.ad).isEqualTo(2)
        assertThat(playingAd2Sample2.adIndex).isNull()
        assertThat(playingAd2Sample2.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingAd2Sample2.customData2).isEqualTo("ad-test-custom-data-2")
        assertThat(playingAd2Sample2.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(playingAd2Sample2.adSystem).isEqualTo("test-ad-system-2")
        assertThat(playingAd2Sample2.adId).isEqualTo("test-ad-id-2")

        val regularPlayingSample = eventDataList[7]
        assertThat(regularPlayingSample.state).isEqualTo("playing")
        assertThat(regularPlayingSample.ad).isEqualTo(0)
        assertThat(regularPlayingSample.adIndex).isNull()
        assertThat(regularPlayingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(regularPlayingSample.customData2).isNull()
        assertThat(regularPlayingSample.adPosition).isNull()
        assertThat(regularPlayingSample.adSystem).isNull()
        assertThat(regularPlayingSample.adId).isNull()
    }

    @Test
    fun test_does_not_ignore_adBreakStart_when_player_is_paused() {
        // arrange
        val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

        mainScope.launch {
            player.pause()
            collector.ssai.adBreakStart(
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
            )
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        mainScope.launch {
            collector.ssai.adStart(
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
            )
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        assertThat(eventDataList.size).isEqualTo(5)
        val startupSample = eventDataList[0]
        assertThat(startupSample.state).isEqualTo("startup")
        assertThat(startupSample.startupTime).isGreaterThan(0)
        assertThat(startupSample.sequenceNumber).isEqualTo(0)
        assertThat(startupSample.ad).isEqualTo(0)
        assertThat(startupSample.adIndex).isNull()
        assertThat(startupSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(startupSample.adPosition).isNull()
        assertThat(startupSample.adSystem).isNull()
        assertThat(startupSample.adId).isNull()

        val playingSample = eventDataList[1]
        assertThat(playingSample.state).isEqualTo("playing")
        assertThat(playingSample.startupTime).isEqualTo(0)
        assertThat(playingSample.sequenceNumber).isEqualTo(1)
        assertThat(playingSample.ad).isEqualTo(0)
        assertThat(playingSample.adIndex).isNull()
        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingSample.adPosition).isNull()
        assertThat(playingSample.adSystem).isNull()
        assertThat(playingSample.adId).isNull()

        val pauseSample = eventDataList[2]
        assertThat(pauseSample.state).isEqualTo("pause")
        assertThat(pauseSample.startupTime).isEqualTo(0)
        assertThat(pauseSample.sequenceNumber).isEqualTo(2)
        assertThat(pauseSample.ad).isEqualTo(0)
        assertThat(pauseSample.adIndex).isNull()
        assertThat(pauseSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(pauseSample.adPosition).isNull()
        assertThat(pauseSample.adSystem).isNull()
        assertThat(pauseSample.adId).isNull()

        val playingAdSample = eventDataList[3]
        assertThat(playingAdSample.state).isEqualTo("playing")
        assertThat(playingAdSample.startupTime).isEqualTo(0)
        assertThat(playingAdSample.sequenceNumber).isEqualTo(3)
        assertThat(playingAdSample.ad).isEqualTo(0)
        assertThat(playingAdSample.adIndex).isNull()
        assertThat(playingAdSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingAdSample.adPosition).isNull()
        assertThat(playingAdSample.adSystem).isNull()
        assertThat(playingAdSample.adId).isNull()

        val playingAdSample2 = eventDataList[4]
        assertThat(playingAdSample2.state).isEqualTo("playing")
        assertThat(playingAdSample2.startupTime).isEqualTo(0)
        assertThat(playingAdSample2.sequenceNumber).isEqualTo(4)
        assertThat(playingAdSample2.ad).isEqualTo(2)
        assertThat(playingAdSample2.adIndex).isEqualTo(0)
        assertThat(playingAdSample2.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(playingAdSample2.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(playingAdSample2.adSystem).isEqualTo("test-ad-system-1")
        assertThat(playingAdSample2.adId).isEqualTo("test-ad-id-1")
    }

    @Test
    fun test_does_not_send_sample_but_sets_metadata_when_adStart_called_with_player_paused() {
        // arrange
        val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            player.play()
            collector.ssai.adBreakStart(
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
            )
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

        mainScope.launch {
            player.pause()
            collector.ssai.adStart(
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
            )
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        assertThat(eventDataList.size).isEqualTo(4)
        val startupSample = eventDataList[0]
        assertThat(startupSample.state).isEqualTo("startup")
        assertThat(startupSample.startupTime).isGreaterThan(0)
        assertThat(startupSample.sequenceNumber).isEqualTo(0)
        assertThat(startupSample.ad).isEqualTo(0)
        assertThat(startupSample.adIndex).isNull()
        assertThat(startupSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(startupSample.adPosition).isNull()
        assertThat(startupSample.adSystem).isNull()
        assertThat(startupSample.adId).isNull()

        val playingSample = eventDataList[1]
        assertThat(playingSample.state).isEqualTo("playing")
        assertThat(playingSample.startupTime).isEqualTo(0)
        assertThat(playingSample.sequenceNumber).isEqualTo(1)
        assertThat(playingSample.ad).isEqualTo(0)
        assertThat(playingSample.adIndex).isNull()
        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingSample.adPosition).isNull()
        assertThat(playingSample.adSystem).isNull()
        assertThat(playingSample.adId).isNull()

        val pauseSample = eventDataList[2]
        assertThat(pauseSample.state).isEqualTo("pause")
        assertThat(pauseSample.startupTime).isEqualTo(0)
        assertThat(pauseSample.sequenceNumber).isEqualTo(2)
        assertThat(pauseSample.ad).isEqualTo(2)
        assertThat(pauseSample.adIndex).isEqualTo(0)
        assertThat(pauseSample.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(pauseSample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(pauseSample.adSystem).isEqualTo("test-ad-system-1")
        assertThat(pauseSample.adId).isEqualTo("test-ad-id-1")

        val playingSample2 = eventDataList[3]
        assertThat(playingSample2.state).isEqualTo("playing")
        assertThat(playingSample2.startupTime).isEqualTo(0)
        assertThat(playingSample2.sequenceNumber).isEqualTo(3)
        assertThat(playingSample2.ad).isEqualTo(2)
        assertThat(playingSample2.adIndex).isNull()
        assertThat(playingSample2.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(playingSample2.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
        assertThat(playingSample2.adSystem).isEqualTo("test-ad-system-1")
        assertThat(playingSample2.adId).isEqualTo("test-ad-id-1")
    }

    @Test
    fun test_does_not_send_sample_but_resets_ssai_related_data_when_adBreakEnd_called_with_player_paused() {
        // arrange
        val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.sourceMetadata = defaultSourceMetadata

        // act
        mainScope.launch {
            collector.attachPlayer(player)
            player.setMediaItem(defaultMediaItem)
            player.prepare()
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

        mainScope.launch {
            collector.ssai.adBreakStart(
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
            )
            collector.ssai.adStart(
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
            )
            player.pause()
            collector.ssai.adBreakEnd()
            player.play()
        }

        Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

        mainScope.launch {
            player.pause()
            collector.detachPlayer()
        }

        Thread.sleep(500)

        // assert
        val impressionList = MockedIngress.extractImpressions()
        assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        val eventDataList = impression.eventDataList
        assertThat(eventDataList.size).isEqualTo(5)
        val startupSample = eventDataList[0]
        assertThat(startupSample.state).isEqualTo("startup")
        assertThat(startupSample.startupTime).isGreaterThan(0)
        assertThat(startupSample.sequenceNumber).isEqualTo(0)
        assertThat(startupSample.ad).isEqualTo(0)
        assertThat(startupSample.adIndex).isNull()
        assertThat(startupSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(startupSample.adPosition).isNull()
        assertThat(startupSample.adSystem).isNull()
        assertThat(startupSample.adId).isNull()

        val playingSample = eventDataList[1]
        assertThat(playingSample.state).isEqualTo("playing")
        assertThat(playingSample.startupTime).isEqualTo(0)
        assertThat(playingSample.sequenceNumber).isEqualTo(1)
        assertThat(playingSample.ad).isEqualTo(0)
        assertThat(playingSample.adIndex).isNull()
        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingSample.adPosition).isNull()
        assertThat(playingSample.adSystem).isNull()
        assertThat(playingSample.adId).isNull()

        val playingSample2 = eventDataList[2]
        assertThat(playingSample2.state).isEqualTo("playing")
        assertThat(playingSample2.startupTime).isEqualTo(0)
        assertThat(playingSample2.sequenceNumber).isEqualTo(2)
        assertThat(playingSample2.ad).isEqualTo(2)
        assertThat(playingSample2.adIndex).isEqualTo(0)
        assertThat(playingSample2.customData1).isEqualTo("ad-test-custom-data-1")
        assertThat(playingSample2.adPosition).isEqualTo("preroll")
        assertThat(playingSample2.adSystem).isEqualTo("test-ad-system-1")
        assertThat(playingSample2.adId).isEqualTo("test-ad-id-1")

        val pauseSample = eventDataList[3]
        assertThat(pauseSample.state).isEqualTo("pause")
        assertThat(pauseSample.startupTime).isEqualTo(0)
        assertThat(pauseSample.sequenceNumber).isEqualTo(3)
        assertThat(pauseSample.ad).isEqualTo(0)
        assertThat(pauseSample.adIndex).isNull()
        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingSample.adPosition).isNull()
        assertThat(playingSample.adSystem).isNull()
        assertThat(playingSample.adId).isNull()

        val playingSample3 = eventDataList[4]
        assertThat(playingSample3.state).isEqualTo("playing")
        assertThat(playingSample3.startupTime).isEqualTo(0)
        assertThat(playingSample3.sequenceNumber).isEqualTo(4)
        assertThat(playingSample3.ad).isEqualTo(0)
        assertThat(playingSample3.adIndex).isNull()
        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
        assertThat(playingSample.adPosition).isNull()
        assertThat(playingSample.adSystem).isNull()
        assertThat(playingSample.adId).isNull()
    }
}
