package com.bitmovin.analytics.bitmovin.player

import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.test.utils.CsaiDataVerifier
import com.bitmovin.analytics.test.utils.DataVerifier
import com.bitmovin.analytics.test.utils.EventDataUtils
import com.bitmovin.analytics.test.utils.MetadataUtils
import com.bitmovin.analytics.test.utils.MockedIngress
import com.bitmovin.analytics.test.utils.PlaybackUtils
import com.bitmovin.analytics.test.utils.TestConfig
import com.bitmovin.analytics.test.utils.TestSources
import com.bitmovin.analytics.test.utils.runBlockingTest
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.AdvertisingConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.source.SourceBuilder
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CsaiScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private var defaultSample = TestSources.HLS_REDBULL
    private var defaultSource = SourceBuilder(SourceConfig.fromUrl(defaultSample.m3u8Url!!)).build()

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private lateinit var mockedIngressUrl: String

    private val progressiveAdSource =
        AdSource(
            AdSourceType.Progressive,
            "https://bitmovin-player-eu-west1-ci-input.s3.amazonaws.com/general/ads/testad2s.mp4",
        )

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        // purging database to have a clean state for each test
        EventDatabaseTestHelper.purge(appContext)
    }

    @After
    fun teardown() {
        MockedIngress.stopServer()
    }

    private fun createPlayer(advertisingConfig: AdvertisingConfig): Player {
        val playbackConfig = PlaybackConfig(isMuted = true, isAutoplayEnabled = true)
        val playerConfig =
            PlayerConfig(
                key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                playbackConfig = playbackConfig,
                advertisingConfig = advertisingConfig,
            )
        val player = Player.create(appContext, playerConfig)
        player.setAdViewGroup(LinearLayout(appContext))
        return player
    }

    @Test
    fun test_vodWithAds_playWithAutoplayAndMuted() =
        runBlockingTest {
            // arrange
            val adSource =
                AdSource(AdSourceType.Progressive, "https://bitmovin-player-eu-west1-ci-input.s3.amazonaws.com/general/ads/testad2s.mp4")
            val preRoll = AdItem("pre", adSource)
            // play midroll after 6 seconds
            val midRoll = AdItem("6", adSource)
            val advertisingConfig = AdvertisingConfig(preRoll, midRoll)
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)

            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val playbackConfig = PlaybackConfig(isMuted = true)
            val playerConfig =
                PlayerConfig(
                    key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                    playbackConfig = playbackConfig,
                    advertisingConfig = advertisingConfig,
                )
            val localPlayer = Player.create(appContext, playerConfig)
            localPlayer.setAdViewGroup(LinearLayout(appContext))
            val sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                collector.setSourceMetadata(defaultSource, sourceMetadata)
                localPlayer.load(defaultSource)
                localPlayer.play()
            }

            // wait until midRoll ad is played
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 7000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            // wait a bit to make sure last play sample is sent
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            Thread.sleep(200)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // verify adSamples
            // we expect 2 adEventData to be sent since there are 2 ads played
            assertThat(impression.adEventDataList.size).isEqualTo(2)
            val firstAd = impression.adEventDataList[0]

            // TODO (AN-4378): what is expected behavior for startupTime? should it be always > 0?
            // This assertions is flaky
            // assertThat(firstAd.adStartupTime).isGreaterThan(0)
            CsaiDataVerifier.verifyStaticAdData(firstAd, analyticsConfig)
            CsaiDataVerifier.verifyFullyPlayedAd(firstAd)

            val secondAd = impression.adEventDataList[1]
            // TODO (AN-4378): what is expected behavior for startupTime? should it be always > 0?
            // This assertions is flaky
            // assertThat(secondAd.adStartupTime).isEqualTo(0)

            CsaiDataVerifier.verifyStaticAdData(secondAd, analyticsConfig)
            CsaiDataVerifier.verifyFullyPlayedAd(secondAd)

            assertThat(firstAd.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)
            assertThat(secondAd.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            // verify samples
            val eventDataWithAdState = impression.eventDataList.filter { x -> x.ad == 1 }
            assertThat(eventDataWithAdState.size).isEqualTo(2)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyStaticData(eventDataList, sourceMetadata, defaultSample, BitmovinPlayerConstants.playerInfo)

            // startup sample is second sample (since order of events in player changed in 3.40.0)
            DataVerifier.verifyStartupSample(eventData = eventDataList[1], expectedSequenceNumber = 1)

            // TODO: we are not collecting videoStart and videoEnd times correctly when ads are played
            // DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyInvariants(eventDataList)

            val filteredList = EventDataUtils.filterNonDeterministicEvents(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(filteredList)
        }

    @Test
    fun test_vodWithPreRollAd_playWithAutoplay() =
        runBlockingTest {
            // arrange
            val preRoll = AdItem("pre", progressiveAdSource)
            val advertisingConfig = AdvertisingConfig(preRoll)
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val localPlayer = createPlayer(advertisingConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                collector.setSourceMetadata(defaultSource, sourceMetadata)
                localPlayer.load(defaultSource)
            }

            // wait for pre-roll ad to start and finish, then wait for content to play
            PlaybackUtils.waitUntil("pre-roll ad started") { localPlayer.isAd }
            PlaybackUtils.waitUntil("pre-roll ad finished") { !localPlayer.isAd }
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            Thread.sleep(200)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig)
            CsaiDataVerifier.verifyFullyPlayedAd(adSample)
            assertThat(adSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSize(1)
        }

    @Test
    fun test_vodWithMidRollAd_playWithAutoplay() =
        runBlockingTest {
            // mid-roll scheduled at 3 seconds into content playback
            val midRoll = AdItem("3", progressiveAdSource)
            val advertisingConfig = AdvertisingConfig(midRoll)
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val localPlayer = createPlayer(advertisingConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                collector.setSourceMetadata(defaultSource, sourceMetadata)
                localPlayer.load(defaultSource)
            }

            // wait for content to pass the mid-roll trigger point and mid-roll to play and finish
            PlaybackUtils.waitUntil("mid-roll ad started") { localPlayer.isAd }
            PlaybackUtils.waitUntil("mid-roll ad finished") { !localPlayer.isAd }

            // wait for content to continue playing after mid-roll
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 5000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            Thread.sleep(200)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            assertThat(impression.adEventDataList).hasSize(1)
            val midRollSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(midRollSample, analyticsConfig)
            CsaiDataVerifier.verifyFullyPlayedAd(midRollSample)
            assertThat(midRollSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSize(1)
        }

    @Test
    fun test_vodWithPostRollAd() =
        runBlockingTest {
            val postRoll = AdItem("post", progressiveAdSource)
            val advertisingConfig = AdvertisingConfig(postRoll)
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val localPlayer = createPlayer(advertisingConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                collector.setSourceMetadata(defaultSource, sourceMetadata)
                localPlayer.load(defaultSource)
            }

            // wait for content to start playing
            BitmovinPlaybackUtils.waitUntilPlaybackStarted(localPlayer)

            withContext(mainScope.coroutineContext) {
                // seek to near end of stream to trigger post-roll ad
                localPlayer.seek(defaultSample.duration / 1000.0 - 2.0)
            }

            // wait for post-roll ad to start and finish
            PlaybackUtils.waitUntil("post-roll ad started") { localPlayer.isAd }
            PlaybackUtils.waitUntil("post-roll ad finished") { !localPlayer.isAd }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            Thread.sleep(200)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            assertThat(impression.adEventDataList).hasSize(1)
            val postRollSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(postRollSample, analyticsConfig)
            CsaiDataVerifier.verifyFullyPlayedAd(postRollSample)
            assertThat(postRollSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSize(1)
        }

    @Test
    fun test_vodWithPreRollAd_closedWhilePlayingAd() =
        runBlockingTest {
            val preRoll = AdItem("pre", progressiveAdSource)
            val advertisingConfig = AdvertisingConfig(preRoll)
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val localPlayer = createPlayer(advertisingConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                collector.setSourceMetadata(defaultSource, sourceMetadata)
                localPlayer.load(defaultSource)
            }

            // wait for pre-roll ad to start and play for a bit
            PlaybackUtils.waitUntil("pre-roll ad started") { localPlayer.isAd }
            Thread.sleep(500)

            // destroy player while ad is still playing
            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig)
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.closed).isEqualTo(1)
            assertThat(adSample.completed).isEqualTo(0)
            assertThat(adSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSize(1)
        }

    @Test
    fun test_vodWithPreRollAd_programChangeCalledWhileAdIsPlaying_shouldNotCreateNewSession() =
        runBlockingTest {
            val preRoll = AdItem("pre", progressiveAdSource)
            val advertisingConfig = AdvertisingConfig(preRoll)
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceMetadataProgram1 =
                SourceMetadata(
                    videoId = "program-1",
                    title = metadataGenerator.getTestTitle(),
                )
            val sourceMetadataProgram2 =
                SourceMetadata(
                    videoId = "new-video-after-program-change",
                    title = "New Video After Program Change",
                )

            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val localPlayer = createPlayer(advertisingConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                collector.setSourceMetadata(defaultSource, sourceMetadataProgram1)
                localPlayer.load(defaultSource)
            }

            // wait for pre-roll to start
            PlaybackUtils.waitUntil("pre-roll ad started") { localPlayer.isAd }

            // call programChange while pre-roll is still playing
            withContext(mainScope.coroutineContext) {
                collector.programChange(sourceMetadataProgram2)
            }

            // wait for pre-roll to finish and content to play
            PlaybackUtils.waitUntil("pre-roll ad finished") { !localPlayer.isAd }
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            Thread.sleep(200)

            // assert - programChange during pre-roll must NOT create a second session
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // the pre-roll ad should still be tracked
            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]
            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig)

            // the ad sample must reference the same impression as the video event data
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            assertThat(adSample.videoImpressionId).isEqualTo(eventDataList.first().impressionId)

            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSize(1)
        }

    @Test
    fun test_vodWithMidRollAd_programChangeCalledWhileAdIsPlaying_adCountedTowardsSecondSession() =
        runBlockingTest {
            // mid-roll scheduled at 3 seconds into content playback
            val midRoll = AdItem("3", progressiveAdSource)
            val advertisingConfig = AdvertisingConfig(midRoll)
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceMetadataProgram1 =
                SourceMetadata(
                    videoId = "program-1",
                    title = "First Program",
                )
            val sourceMetadataProgram2 =
                SourceMetadata(
                    videoId = "program-2",
                    title = "Second Program",
                )

            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val localPlayer = createPlayer(advertisingConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                collector.setSourceMetadata(defaultSource, sourceMetadataProgram1)
                localPlayer.load(defaultSource)
            }

            // wait for mid-roll ad to start (triggered at 3s of content)
            PlaybackUtils.waitUntil("mid-roll ad started") { localPlayer.isAd }

            // call programChange while the mid-roll is still playing
            withContext(mainScope.coroutineContext) {
                collector.programChange(sourceMetadataProgram2)
            }

            // wait for mid-roll to finish and content to continue
            PlaybackUtils.waitUntil("mid-roll ad finished") { !localPlayer.isAd }
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 5000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            Thread.sleep(200)

            // assert - programChange during mid-roll must create two sessions
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(2)

            // session 1: content playback before the mid-roll, no ad sample
            val impression1 = impressionList[0]
            DataVerifier.verifyHasNoErrorSamples(impression1)
            assertThat(impression1.adEventDataList).isEmpty()

            val eventDataList1 = impression1.eventDataList
            DataVerifier.verifyInvariants(eventDataList1)
            assertThat(eventDataList1.first().videoId).isEqualTo(sourceMetadataProgram1.videoId)
            assertThat(eventDataList1.first().isProgramChange).isNull()

            // session 2: mid-roll ad belongs here, followed by content
            val impression2 = impressionList[1]
            DataVerifier.verifyHasNoErrorSamples(impression2)
            assertThat(impression2.adEventDataList).hasSize(1)

            val adSample = impression2.adEventDataList[0]
            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig)

            val eventDataList2 = impression2.eventDataList
            DataVerifier.verifyInvariants(eventDataList2)
            assertThat(eventDataList2.first().videoId).isEqualTo(sourceMetadataProgram2.videoId)
            assertThat(eventDataList2.first().isProgramChange).isTrue()

            // the ad sample must be linked to the second session's impression
            val impressionId1 = eventDataList1.first().impressionId
            val impressionId2 = eventDataList2.first().impressionId
            assertThat(impressionId2).isNotEqualTo(impressionId1)
            assertThat(adSample.videoImpressionId).isEqualTo(impressionId2)
        }

    @Test
    fun test_vodWithImaPreRollAd_playWithAutoplay() =
        runBlockingTest {
            val imaAdSource = AdSource(AdSourceType.Ima, TestSources.IMA_AD_SOURCE_2)
            val preRoll = AdItem("pre", imaAdSource)
            val advertisingConfig = AdvertisingConfig(preRoll)
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val localPlayer = createPlayer(advertisingConfig)

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(localPlayer)
                collector.setSourceMetadata(defaultSource, sourceMetadata)
                localPlayer.load(defaultSource)
            }

            // wait for pre-roll ad to start and finish, then wait for content to play
            PlaybackUtils.waitUntil("pre-roll ad started") { localPlayer.isAd }
            PlaybackUtils.waitUntil("pre-roll ad finished") { !localPlayer.isAd }
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                localPlayer.pause()
            }

            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            Thread.sleep(200)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig)
            CsaiDataVerifier.verifyFullyPlayedAd(adSample)

            assertThat(adSample.adStartupTime).isGreaterThan(0)
            assertThat(adSample.timePlayed).isGreaterThan(9000)

            // IMA-specific fields
            // TODO: add more fields to be checked
            assertThat(adSample.adPosition).isEqualTo("pre")
            assertThat(adSample.manifestDownloadTime).isGreaterThan(0)
            assertThat(adSample.isLinear).isTrue()
            assertThat(adSample.adTitle).isEqualTo("External NCA1C1L1 Linear Inline")
            assertThat(adSample.adDuration).isEqualTo(10000)
            assertThat(adSample.creativeId).isNotEmpty()
            assertThat(adSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSize(1)
        }

    @Test
    fun test_vodWithImaPreRollAdError_sendsAdErrorSample() =
        runBlockingTest {
            // IMA_AD_SOURCE_1 is a "redirecterror" tag that results in an ad error
            val imaAdSource = AdSource(AdSourceType.Ima, TestSources.IMA_AD_SOURCE_1)
            val preRoll = AdItem("pre", imaAdSource)
            val advertisingConfig = AdvertisingConfig(preRoll)
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())
            val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
            val localPlayer = createPlayer(advertisingConfig)

            var adErrorOccurred = false

            // act
            withContext(mainScope.coroutineContext) {
                localPlayer.on(PlayerEvent.AdError::class) { adErrorOccurred = true }

                collector.attachPlayer(localPlayer)
                collector.setSourceMetadata(defaultSource, sourceMetadata)
                localPlayer.load(defaultSource)
            }

            PlaybackUtils.waitUntil("ad error occurred") { adErrorOccurred }
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                localPlayer.destroy()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()

            // verify 1 ad error sample is sent
            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig)
            // TODO: verify more fields
            assertThat(adSample.errorCode).isNotNull
            assertThat(adSample.errorMessage).isNotBlank
            assertThat(adSample.closed).isEqualTo(0)
        }

    @Test
    fun test_vodWithImaPreRollAd_vastMediaError_sendsAdErrorSample() =
        runBlockingTest {
            // Uses a custom locally-served VAST with an HLS ad pointing to a segment that 404s.
            // IMA preloads ads before the break starts, so AD_BREAK_BEGIN and AD_BEGIN may not fire
            // before the error — but the error IS tracked and the ad sample IS sent.
            val adServer = MockWebServer()
            try {
                adServer.start()

                val hlsPlaylistUrl = adServer.url("/ad.m3u8").toString()
                val hlsPlaylist =
                    """
                    #EXTM3U
                    #EXT-X-VERSION:3
                    #EXT-X-TARGETDURATION:10
                    #EXT-X-MEDIA-SEQUENCE:0
                    #EXTINF:10.0,
                    ${adServer.url("/segment0.ts")}
                    #EXT-X-ENDLIST
                    """.trimIndent()
                val vastXml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <VAST version="2.0">
                      <Ad id="error-ad">
                        <InLine>
                          <AdSystem>Test</AdSystem>
                          <AdTitle>Test Error Ad</AdTitle>
                          <Impression><![CDATA[]]></Impression>
                          <Creatives>
                            <Creative>
                              <Linear>
                                <Duration>00:00:10</Duration>
                                <MediaFiles>
                                  <MediaFile type="application/x-mpegURL" width="640" height="360" delivery="streaming">
                                    <![CDATA[$hlsPlaylistUrl]]>
                                  </MediaFile>
                                </MediaFiles>
                              </Linear>
                            </Creative>
                          </Creatives>
                        </InLine>
                      </Ad>
                    </VAST>
                    """.trimIndent()

                adServer.dispatcher =
                    object : Dispatcher() {
                        override fun dispatch(request: RecordedRequest): MockResponse =
                            when {
                                request.path?.startsWith("/vast") == true ->
                                    MockResponse().setResponseCode(200).setBody(vastXml)
                                request.path?.endsWith(".m3u8") == true ->
                                    MockResponse().setResponseCode(200)
                                        .addHeader("Content-Type", "application/x-mpegURL")
                                        .setBody(hlsPlaylist)
                                else ->
                                    // Segment returns 404 - fails when IMA tries to play
                                    MockResponse().setResponseCode(404)
                            }
                    }

                val imaAdSource = AdSource(AdSourceType.Ima, adServer.url("/vast").toString())
                val preRoll = AdItem("pre", imaAdSource)
                val advertisingConfig = AdvertisingConfig(preRoll)
                val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
                val sourceMetadata = SourceMetadata(title = metadataGenerator.getTestTitle())
                val collector = IBitmovinPlayerCollector.create(appContext, analyticsConfig)
                val localPlayer = createPlayer(advertisingConfig)

                var adErrorOccurred = false

                withContext(mainScope.coroutineContext) {
                    localPlayer.on(PlayerEvent.AdError::class) { adErrorOccurred = true }

                    collector.attachPlayer(localPlayer)
                    collector.setSourceMetadata(defaultSource, sourceMetadata)
                    localPlayer.load(defaultSource)
                }

                PlaybackUtils.waitUntil("ad error occurred") { adErrorOccurred }
                Thread.sleep(500)

                withContext(mainScope.coroutineContext) {
                    collector.detachPlayer()
                    localPlayer.destroy()
                }

                Thread.sleep(500)

                // assert
                val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
                assertThat(impressionList).hasSize(1)

                val impression = impressionList.first()
                assertThat(impression.adEventDataList).hasSize(1)
                val adSample = impression.adEventDataList[0]

                CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig)
                assertThat(adSample.errorCode).isNotNull
                assertThat(adSample.errorMessage).isNotBlank
                // TODO: add tag type
            } finally {
                adServer.shutdown()
            }
        }
}
