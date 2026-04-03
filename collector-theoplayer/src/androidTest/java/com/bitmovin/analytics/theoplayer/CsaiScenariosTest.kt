package com.bitmovin.analytics.theoplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.test.utils.CsaiDataVerifier
import com.bitmovin.analytics.test.utils.DataVerifier
import com.bitmovin.analytics.test.utils.MetadataUtils
import com.bitmovin.analytics.test.utils.MockedIngress
import com.bitmovin.analytics.test.utils.PlaybackUtils
import com.bitmovin.analytics.test.utils.TestConfig
import com.bitmovin.analytics.test.utils.TestSources
import com.bitmovin.analytics.test.utils.runBlockingTest
import com.bitmovin.analytics.theoplayer.api.ITHEOplayerCollector
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ima.GoogleImaIntegrationFactory
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.ads.AdErrorEvent
import com.theoplayer.android.api.event.ads.AdsEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CsaiScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private var defaultSourceMetadata: SourceMetadata
        get() =
            SourceMetadata(
                title = metadataGenerator.getTestTitle(),
                videoId = "csaiTest",
            )
        set(_) {}

    private lateinit var player: Player
    private lateinit var theoPlayerView: THEOplayerView
    private lateinit var mockedIngressUrl: String

    // BigBuckBunny DASH source used as content in ad tests
    private val contentSource =
        TypedSource
            .Builder(TestSources.THEO_BIGBUCKBUNNY.mpdUrl!!)
            .type(SourceType.DASH)
            .build()

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()

        val playerConfig =
            THEOplayerConfig.Builder()
                .license(TheoPlayerTestUtils.TESTING_LICENSE)
                .build()

        runBlocking {
            withContext(mainScope.coroutineContext) {
                theoPlayerView = THEOplayerView(appContext, playerConfig)
                player = theoPlayerView.player
                // IMA integration must be explicitly registered for GoogleImaAdDescription to work
                val imaIntegration = GoogleImaIntegrationFactory.createGoogleImaIntegration(theoPlayerView)
                player.addIntegration(imaIntegration)
                player.useLowestRendition()
                player.isAutoplay = true
                player.volume = 0.0
            }
        }
    }

    @After
    fun teardown() {
        runBlocking {
            withContext(mainScope.coroutineContext) {
                if (!theoPlayerView.isDestroyed) {
                    theoPlayerView.onDestroy()
                }
            }
        }
        MockedIngress.stopServer()
    }

    @Test
    fun test_vodWithPreRollAd_playWithAutoplay() =
        runBlockingTest {
            // arrange
            val preRollAd1 =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_2)
                    .timeOffset("start")
                    .build()

            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(preRollAd1)
                    .build()

            // act
            withContext(mainScope.coroutineContext) {
                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = sourceDescription
            }

            // wait for the pre-roll ad to start and finish, then wait for content to play
            PlaybackUtils.waitUntil("pre-roll ad started") { player.ads.isPlaying }
            PlaybackUtils.waitUntil("pre-roll ad finished") { !player.ads.isPlaying }
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            // wait a bit to make sure the last play sample is sent
            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // verify 1 ad sample for the pre-roll
            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            CsaiDataVerifier.verifyFullyPlayedAd(adSample)
            assertThat(adSample.adPosition).isEqualTo("pre")
            assertThat(adSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            // verify ad specific fields that we expect to be set
            assertThat(adSample.adModule).isEqualTo("google-ima")
            assertThat(adSample.isLinear).isTrue
            assertThat(adSample.creativeId).isNotEmpty
            assertThat(adSample.adTitle).isEqualTo("External NCA1C1L1 Linear Inline")
            assertThat(adSample.adDescription).isEqualTo("External NCA1C1L1 Linear Inline ad")
            assertThat(adSample.adDuration).isEqualTo(10000)
            assertThat(adSample.timePlayed).isGreaterThan(9000)
            assertThat(adSample.adSystem).isEqualTo("GDFP")
            assertThat(adSample.universalAdIdValue).isNotEmpty
            assertThat(adSample.universalAdIdRegistry).isNotEmpty
            assertThat(adSample.adStartupTime).isGreaterThan(0)
            assertThat(adSample.videoBitrate).isGreaterThan(0)
            assertThat(adSample.streamFormat).isEqualTo("video/mp4")

            // verify event data is linked to the ad
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithClientSideAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithClientSideAdState).hasSize(1)

            DataVerifier.verifyStartupSampleIsSentAfterPreRollAd(impression)
        }

    @Test
    @Ignore("this test is mainly used for manual verification of ad startuptime tracking")
    fun test_vodWithPreRollAd_playWithoutAutoplay() =
        runBlockingTest {
            // arrange
            val preRollAd1 =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_2)
                    .timeOffset("start")
                    .build()

            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(preRollAd1)
                    .build()

            // act
            withContext(mainScope.coroutineContext) {
                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                player.isAutoplay = false
                collector.attachPlayer(player)
                player.source = sourceDescription
            }

            // wait a bit before starting (seems like there is no proper event)
            Thread.sleep(2000)
            withContext(mainScope.coroutineContext) {
                player.play()
            }

            // wait for the pre-roll ad to start and finish, then wait for content to play
            PlaybackUtils.waitUntil("pre-roll ad started") { player.ads.isPlaying }
            PlaybackUtils.waitUntil("pre-roll ad finished") { !player.ads.isPlaying }
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            // wait a bit to make sure the last play sample is sent
            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // verify 1 ad sample for the pre-roll
            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            CsaiDataVerifier.verifyFullyPlayedAd(adSample)
            assertThat(adSample.adPosition).isEqualTo("pre")
            assertThat(adSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)
            assertThat(adSample.adStartupTime).isGreaterThan(0)

            // verify event data is linked to the ad
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithClientSideAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithClientSideAdState).hasSize(1)

            DataVerifier.verifyStartupSampleIsSentAfterPreRollAd(impression)
        }

    @Test
    fun test_vodWithPreRollAd_programChangeCalledWhileAdIsPlaying_shouldNotCreateNewSession() =
        runBlockingTest {
            // arrange
            val preRollAd =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_2)
                    .timeOffset("start")
                    .build()
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(preRollAd)
                    .build()
            val newSourceMetadata =
                SourceMetadata(
                    videoId = "new-video-after-program-change",
                    title = "New Video After Program Change",
                )

            var collector: ITHEOplayerCollector? = null

            // act
            withContext(mainScope.coroutineContext) {
                collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = sourceDescription
            }

            // wait for the pre-roll ad to start
            PlaybackUtils.waitUntil("pre-roll ad started") { player.ads.isPlaying }

            // call programChange while the ad is still playing
            withContext(mainScope.coroutineContext) {
                collector!!.programChange(newSourceMetadata)
            }

            // wait for the pre-roll ad to finish and content to play
            PlaybackUtils.waitUntil("pre-roll ad finished") { !player.ads.isPlaying }
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            // assert - programChange during ad must NOT create a second session
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // the pre-roll ad should still be tracked
            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]
            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            assertThat(adSample.adPosition).isEqualTo("pre")

            // the ad sample must reference the same impression as the video event data
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            assertThat(adSample.videoImpressionId).isEqualTo(eventDataList.first().impressionId)

            // the pre-roll ad should be the first (startup) sample of the session
            val firstEventData = eventDataList.first()
            assertThat(firstEventData.sequenceNumber).isEqualTo(0)
            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSize(1)

            DataVerifier.verifyStartupSampleIsSentAfterPreRollAd(impression)
        }

    @Test
    fun test_vodWithMidRollAd_programChangeCalledWhileAdIsPlaying_adCountedTowardsSecondSession() =
        runBlockingTest {
            // arrange
            // midroll scheduled at 2 seconds into content playback
            val midRollAd =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_2)
                    .timeOffset("2")
                    .build()
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(midRollAd)
                    .build()
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

            var collector: ITHEOplayerCollector? = null

            // act
            withContext(mainScope.coroutineContext) {
                collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = sourceMetadataProgram1
                collector.attachPlayer(player)
                player.source = sourceDescription
            }

            // wait for mid-roll ad to start (triggered at 2s of content)
            PlaybackUtils.waitUntil("mid-roll ad started") { player.ads.isPlaying }

            // call programChange while the mid-roll is still playing
            withContext(mainScope.coroutineContext) {
                collector!!.programChange(sourceMetadataProgram2)
            }

            // wait for mid-roll to finish and content to continue in the new session
            PlaybackUtils.waitUntil("mid-roll ad finished") { !player.ads.isPlaying }
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

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
            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)

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
    fun test_vodWithPreRollAd_closedWhilePlayingAd() =
        runBlockingTest {
            // arrange
            val preRollAd =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_2)
                    .timeOffset("start")
                    .build()
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(preRollAd)
                    .build()

            // act
            withContext(mainScope.coroutineContext) {
                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = sourceDescription
            }

            PlaybackUtils.waitUntil("pre-roll ad started") { player.ads.isPlaying }
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            withContext(mainScope.coroutineContext) {
                theoPlayerView.onDestroy()
            }

            // wait a bit to make sure the last play sample is sent
            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // verify 1 ad sample for the pre-roll
            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.closed).isEqualTo(1)
            assertThat(adSample.completed).isEqualTo(0)
            assertThat(adSample.adPosition).isEqualTo("pre")
            assertThat(adSample.playPercentage).isBetween(1, 99)
            assertThat(adSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            // expect event data sample reflecting the ad
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithClientSideAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithClientSideAdState).hasSize(1)
        }

    @Test
    fun test_vodWithPreRollAndMidRollAd_playWithAutoplay() =
        runBlockingTest {
            // arrange
            val preRollAd =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_3)
                    .timeOffset("start")
                    .build()
            // midroll scheduled at 2 seconds into content playback
            val midRollAd =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_2)
                    .timeOffset("2")
                    .build()
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(preRollAd, midRollAd)
                    .build()

            // act
            withContext(mainScope.coroutineContext) {
                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = sourceDescription
            }

            // wait for pre-roll to start and finish
            PlaybackUtils.waitUntil("pre-roll ad started") { player.ads.isPlaying }
            PlaybackUtils.waitUntil("pre-roll ad finished") { !player.ads.isPlaying }

            Thread.sleep(1000)

            // wait for content to pass the mid-roll trigger point and mid-roll to play and finish
            PlaybackUtils.waitUntil("mid-roll ad started") { player.ads.isPlaying }
            PlaybackUtils.waitUntil("mid-roll ad finished") { !player.ads.isPlaying }

            // wait for content to continue playing after mid-roll
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

            withContext(mainScope.coroutineContext) {
                player.pause()
                theoPlayerView.onDestroy()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // 2 ad samples: one pre-roll and one mid-roll
            assertThat(impression.adEventDataList).hasSize(2)

            val preRollSample = impression.adEventDataList[0]
            CsaiDataVerifier.verifyStaticAdData(preRollSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            CsaiDataVerifier.verifyFullyPlayedAd(preRollSample)
            assertThat(preRollSample.adPosition).isEqualTo("pre")

            val midRollSample = impression.adEventDataList[1]
            CsaiDataVerifier.verifyStaticAdData(midRollSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            CsaiDataVerifier.verifyFullyPlayedAd(midRollSample)

            assertThat(midRollSample.adPosition).isEqualTo("mid")

            assertThat(preRollSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)
            assertThat(midRollSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSizeGreaterThanOrEqualTo(2)

            DataVerifier.verifyStartupSampleIsSentAfterPreRollAd(impression)
        }

    @Test
    fun test_vodWithMidRollAd() =
        runBlockingTest {
            // midroll scheduled at 2 seconds into content playback
            val midRollAd =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_2)
                    .timeOffset("2")
                    .build()
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(midRollAd)
                    .build()

            // act
            withContext(mainScope.coroutineContext) {
                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = sourceDescription
            }

            // wait for content to pass the mid-roll trigger point and mid-roll to play and finish
            PlaybackUtils.waitUntil("mid-roll ad started") { player.ads.isPlaying }
            PlaybackUtils.waitUntil("mid-roll ad finished") { !player.ads.isPlaying }

            // wait for content to continue playing after mid-roll
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                theoPlayerView.onDestroy()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // 1 ad samples: one mid-roll
            assertThat(impression.adEventDataList).hasSize(1)

            val midRollSample = impression.adEventDataList[0]
            CsaiDataVerifier.verifyStaticAdData(midRollSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            CsaiDataVerifier.verifyFullyPlayedAd(midRollSample)

            assertThat(midRollSample.adPosition).isEqualTo("mid")
            assertThat(midRollSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSize(1)
        }

    @Test
    fun test_vodWithPostRollAd() =
        runBlockingTest {
            val postRollAd =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_2)
                    .timeOffset("end")
                    .build()
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(postRollAd)
                    .build()

            // act
            withContext(mainScope.coroutineContext) {
                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = sourceDescription
            }

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            withContext(mainScope.coroutineContext) {
                // seek to end of stream in order to trigger postroll ad
                player.currentTime = player.duration - 1.0
            }

            // wait for content to pass the post-roll trigger point and post-roll to play and finish
            PlaybackUtils.waitUntil("post-roll ad started") { player.ads.isPlaying }
            PlaybackUtils.waitUntil("post-roll ad finished") { !player.ads.isPlaying }

            withContext(mainScope.coroutineContext) {
                theoPlayerView.onDestroy()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // 1 ad samples: one post-roll
            assertThat(impression.adEventDataList).hasSize(1)

            val postRollSample = impression.adEventDataList[0]
            CsaiDataVerifier.verifyStaticAdData(postRollSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            CsaiDataVerifier.verifyFullyPlayedAd(postRollSample)

            assertThat(postRollSample.adPosition).isEqualTo("post")
            assertThat(postRollSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSize(1)
        }

    @Test
    fun test_vodWithSkippablePreRollAd_adIsPlayedFully() =
        runBlockingTest {
            // arrange
            // IMA_AD_SOURCE_3 is a skippable linear ad (5-second skip offset, ~15s duration)
            val preRollAd =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_3)
                    .timeOffset("start")
                    .build()
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(preRollAd)
                    .build()

            // act
            withContext(mainScope.coroutineContext) {
                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = sourceDescription
            }

            // wait for the pre-roll ad to start playing
            PlaybackUtils.waitUntil("pre-roll ad started") { player.ads.isPlaying }

            // wait for the skip button to become available (5-second skip offset)
            Thread.sleep(6000)

            withContext(mainScope.coroutineContext) {
//                skipping doesn't work through the API anymore according to theo folk
//                player.ads.skip()
            }

            // wait for the ad to stop
            PlaybackUtils.waitUntil("pre-roll ad skipped") { !player.ads.isPlaying }

            // wait a bit for content to start and sample to be sent
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 500)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            assertThat(adSample.adPosition).isEqualTo("pre")
            assertThat(adSample.started).isEqualTo(1)

            // this test only verifies that we track the skip metadata correctly
            // the skip api call is a noop thus we cannot automate testing the skipping
            assertThat(adSample.adSkippable).isTrue()
            assertThat(adSample.adSkippableAfter).isGreaterThan(0)
            assertThat(adSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)
            assertThat(adSample.completed).isEqualTo(1)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithClientSideAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithClientSideAdState).hasSize(1)
        }

    @Test
    fun test_vodWithVmapMidRollPod_twoAdsPlayedSequentially_adPodPositionIncreases() =
        runBlockingTest {
            // arrange
            // VMAP tag that returns a single mid-roll break with a pod of 2 linear ads
            val vmapAd =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_VMAP_MIDROLL_2ADS)
                    .build()

            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(vmapAd)
                    .build()

            // act
            withContext(mainScope.coroutineContext) {
                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = sourceDescription
                // ad starts at 15th second, thus seeking to 14
                player.currentTime = 14.0
            }

            // wait for first pre-roll ad to start and finish
            PlaybackUtils.waitUntil("midRoll ad bread started") { player.ads.isPlaying }
            PlaybackUtils.waitUntil("midRoll ad bread finished") { !player.ads.isPlaying }

            // wait for content to play briefly after the ad break
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 16000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // both mid-roll ads from the VMAP pod must be tracked
            assertThat(impression.adEventDataList).hasSize(2)

            val firstAdSample = impression.adEventDataList[0]
            CsaiDataVerifier.verifyStaticAdData(firstAdSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            CsaiDataVerifier.verifyFullyPlayedAd(firstAdSample)

//            TODO: player reports offset=0 for this vmap asset, reported it to theo
//            assertThat(firstAdSample.adPosition).isEqualTo("mid")
            assertThat(firstAdSample.adPodPosition).isEqualTo(0)
            assertThat(firstAdSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            val secondAdSample = impression.adEventDataList[1]
            CsaiDataVerifier.verifyStaticAdData(secondAdSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            CsaiDataVerifier.verifyFullyPlayedAd(secondAdSample)
//            TODO: player reports offset=0 for this vmap asset, reported it to theo
//            assertThat(secondAdSample.adPosition).isEqualTo("mid")
            // adPodPosition must be higher than the first ad, confirming it increments within the break
            assertThat(secondAdSample.adPodPosition).isEqualTo(1)
            assertThat(secondAdSample.videoImpressionId).isEqualTo(impression.eventDataList[0].impressionId)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            val eventDataWithAdState = eventDataList.filter { it.ad == 1 }
            assertThat(eventDataWithAdState).hasSize(1)
        }

    @Test
    fun test_vodWithPreRollAdError_sendsAdErrorSample() =
        runBlockingTest {
            // arrange
            // IMA_AD_SOURCE_1 is a "redirecterror" tag that results in an ad error
            val preRollAd =
                GoogleImaAdDescription
                    .Builder(TestSources.IMA_AD_SOURCE_1)
                    .timeOffset("start")
                    .build()
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            val sourceDescription =
                SourceDescription
                    .Builder(contentSource)
                    .ads(preRollAd)
                    .build()

            var adErrorOccurred = false

            // act
            withContext(mainScope.coroutineContext) {
                player.ads.addEventListener(AdsEventTypes.AD_ERROR, EventListener<AdErrorEvent> { adErrorOccurred = true })

                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = sourceDescription
            }

            PlaybackUtils.waitUntil("ad error occurred") { adErrorOccurred }
            Thread.sleep(500)

            withContext(mainScope.coroutineContext) {
                theoPlayerView.onDestroy()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)

            val impression = impressionList.first()

            // verify 1 ad error sample is sent
            assertThat(impression.adEventDataList).hasSize(1)
            val adSample = impression.adEventDataList[0]

            CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
            assertThat(adSample.errorCode).isNotNull
            assertThat(adSample.errorMessage).isNotBlank
            assertThat(adSample.closed).isEqualTo(0)
        }

    @Test
    fun test_vodWithPreRollAd_vastMediaError_sendsAdErrorSample() =
        runBlockingTest {
            // Uses a custom locally-served VAST with an HLS ad pointing to a segment that 404s.
            // IMA preloads ads before the break starts, so AD_BREAK_BEGIN and AD_BEGIN do not fire
            // before the error — but the error IS tracked and the ad sample IS sent.
            val adServer = MockWebServer()
            try {
                adServer.start()

                val hlsPlaylistUrl = adServer.url("/ad.m3u8").toString()
                // HLS playlist points to a segment that returns garbage — IMA streams HLS
                // so it fires AD_BREAK_BEGIN and AD_BEGIN before requesting the first segment
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
                                    // Segment returns 404 - fails only when IMA tries to play
                                    MockResponse().setResponseCode(404)
                            }
                    }

                val preRollAd =
                    GoogleImaAdDescription
                        .Builder(adServer.url("/vast").toString())
                        .timeOffset("start")
                        .build()
                val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
                val sourceDescription =
                    SourceDescription
                        .Builder(contentSource)
                        .ads(preRollAd)
                        .build()

                var adErrorOccurred = false

                withContext(mainScope.coroutineContext) {
                    player.ads.addEventListener(AdsEventTypes.AD_ERROR, EventListener<AdErrorEvent> { adErrorOccurred = true })

                    val collector = ITHEOplayerCollector.create(appContext, analyticsConfig)
                    collector.sourceMetadata = defaultSourceMetadata
                    collector.attachPlayer(player)
                    player.source = sourceDescription
                }

                PlaybackUtils.waitUntil("ad error occurred") { adErrorOccurred }
                Thread.sleep(500)

                withContext(mainScope.coroutineContext) {
                    theoPlayerView.onDestroy()
                }
                Thread.sleep(500)

                // assert
                val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
                assertThat(impressionList).hasSize(1)

                val impression = impressionList.first()
                assertThat(impression.adEventDataList).hasSize(1)
                val adSample = impression.adEventDataList[0]

                CsaiDataVerifier.verifyStaticAdData(adSample, analyticsConfig, TheoPlayerConstants.playerInfo.playerName)
                assertThat(adSample.errorCode).isNotNull
                assertThat(adSample.errorMessage).isNotBlank
            } finally {
                adServer.shutdown()
            }
        }
}
