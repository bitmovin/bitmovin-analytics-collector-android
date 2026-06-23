package com.bitmovin.analytics.theoplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.test.utils.DataVerifier
import com.bitmovin.analytics.test.utils.MetadataUtils
import com.bitmovin.analytics.test.utils.MockedIngress
import com.bitmovin.analytics.test.utils.PlaybackUtils
import com.bitmovin.analytics.test.utils.SsaiDataVerifier
import com.bitmovin.analytics.test.utils.TestConfig
import com.bitmovin.analytics.test.utils.TestSources
import com.bitmovin.analytics.test.utils.runBlockingTest
import com.bitmovin.analytics.theoplayer.api.ITHEOplayerCollector
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceType
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * SSAI system tests for THEOplayer that drive ad breaks deterministically through a custom
 * server-side ad integration ([TestSsaiAdIntegration]). Unlike [SsaiScenariosTest] - which relies
 * on a real Google DAI stream
 */
@RunWith(AndroidJUnit4::class)
class CustomSsaiScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    // FIXME: verify existing test cases a bit more since they are claude generated

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private val defaultSourceMetadata: SourceMetadata
        get() =
            SourceMetadata(
                title = metadataGenerator.getTestTitle(),
                videoId = "ssaiCustomIntegrationTest",
                path = "testPath",
            )

    private val defaultMetadata = DefaultMetadata(cdnProvider = "cdnProvider", customUserId = "customUserId1")

    // Plain playable content the custom integration resolves the marked source to.
    private val contentUrl = TestSources.HLS_REDBULL.m3u8Url!!
    private val contentType = SourceType.HLS

    private lateinit var player: Player
    private lateinit var theoPlayerView: THEOplayerView
    private lateinit var mockedIngressUrl: String
    private lateinit var ssaiIntegration: TestSsaiAdIntegration

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
                player.useLowestRendition()
                player.volume = 0.0
            }
        }

        ssaiIntegration = TestSsaiAdIntegration(mainScope)
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

    /**
     * Creates and attaches a collector, registers the custom SSAI integration on the player and
     * waits until the integration is ready to drive ad breaks. Shared by every scenario.
     */
    private suspend fun attachCollectorAndWaitUntilSsaiReady(ssaiEngagementTrackingEnabled: Boolean = true) {
        val analyticsConfig =
            TestConfig.createAnalyticsConfig(
                backendUrl = mockedIngressUrl,
                ssaiEngagementTrackingEnabled = ssaiEngagementTrackingEnabled,
            )
        withContext(mainScope.coroutineContext) {
            val collector = ITHEOplayerCollector.create(appContext, analyticsConfig, defaultMetadata)
            collector.sourceMetadata = defaultSourceMetadata
            collector.attachPlayer(player)
            player.isAutoplay = true
        }
        ssaiIntegration.register(player, contentUrl, contentType)
        PlaybackUtils.waitUntil("ssai integration ready") { ssaiIntegration.isReady }
    }

    @Test
    fun test_customSsai_singleAdBreak_singleAd_tracksAllQuartiles() =
        runBlockingTest {
            // arrange + act
            attachCollectorAndWaitUntilSsaiReady()

            // let regular content play before the ad break
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            // drive a single pre-roll-style ad break with one ad through all quartiles
            ssaiIntegration.playAdBreak(timeOffset = 0, adCount = 1)

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

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

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            val adSample = adEventDataList[0]
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.quartile1).isEqualTo(1)
            assertThat(adSample.midpoint).isEqualTo(1)
            assertThat(adSample.quartile3).isEqualTo(1)
            assertThat(adSample.completed).isEqualTo(1)
            // the ad duration set on the AdInit (5s) is extracted and reported in milliseconds
            assertThat(adSample.adDuration).isEqualTo(5000)

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(adEventDataList, 0)
            SsaiDataVerifier.verifySamplesHaveSameAdId(adEventDataList, "test-ssai-0-0")
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)

            val impressionId = impression.eventDataList.first().impressionId
            adEventDataList.forEach {
                assertThat(it.videoImpressionId).isEqualTo(impressionId)
            }
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        }

    @Test
    fun test_customSsai_engagementTrackingDisabled_doesNotTrackAdSamples() =
        runBlockingTest {
            // arrange + act: with SSAI engagement tracking disabled, no SSAI ad samples must be sent
            attachCollectorAndWaitUntilSsaiReady(ssaiEngagementTrackingEnabled = false)

            // let regular content play before the ad break
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            // drive a full single-ad break through all quartiles (same as the tracked scenario)
            ssaiIntegration.playAdBreak(timeOffset = 0, adCount = 1)

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

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

            // engagement tracking is off -> no ad engagement samples are produced
            assertThat(impression.adEventDataList).isEmpty()

            // content is still tracked and the regular event data is still marked as SSAI related
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        }

    @Test
    fun test_customSsai_abandonDuringAd_marksExitedAdBreak() =
        runBlockingTest {
            // arrange + act
            attachCollectorAndWaitUntilSsaiReady()
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            // begin an ad break with one ad and leave it in progress
            ssaiIntegration.beginOpenAdBreak(timeOffset = 0)

            // let the ad play for a bit, then abandon by destroying the player
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                if (!theoPlayerView.isDestroyed) {
                    theoPlayerView.onDestroy()
                }
            }
            // wait a bit to make sure the last play sample is sent
            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)
            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            val adSample = adEventDataList.first()
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.completed).isEqualTo(0)
            assertThat(adSample.exitedAdBreak).isTrue
            assertThat(adSample.adId).isEqualTo("test-ssai-open-0")
            assertThat(adSample.adDuration).isEqualTo(5000)

            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)

            val impressionId = impression.eventDataList.first().impressionId
            adEventDataList.forEach {
                assertThat(it.videoImpressionId).isEqualTo(impressionId)
            }
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        }

    @Test
    fun test_customSsai_midRollAdBreak_hasContentSamplesBeforeAndAfterAd() =
        runBlockingTest {
            // arrange + act
            attachCollectorAndWaitUntilSsaiReady()

            // play content first, so the ad break is a mid-roll
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1000)

            ssaiIntegration.playAdBreak(timeOffset = 5, adCount = 1)

            // continue content playback after the ad break
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

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

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)
            assertThat(adEventDataList.first().completed).isEqualTo(1)
            assertThat(adEventDataList.first().adDuration).isEqualTo(5000)
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)

            // there must be content samples both before and after the SSAI ad
            val eventDataList = impression.eventDataList
            assertThat(DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)).isNotEmpty
            assertThat(DataVerifier.getAllSamplesAfterSsaiAdWithIndex(eventDataList, 0)).isNotEmpty

            val eventDataList1 = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList1)
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(eventDataList1)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList1)
        }

    @Test
    fun test_customSsai_multipleAdBreaks_preMidMidPostRoll_tracksEachBreak() =
        runBlockingTest {
            // arrange + act
            attachCollectorAndWaitUntilSsaiReady()

            // pre-roll: a single ad break right at the start of playback
            ssaiIntegration.playAdBreak(timeOffset = 0, adCount = 1)

            // play content, then a first mid-roll
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)
            ssaiIntegration.playAdBreak(timeOffset = 5, adCount = 2, durationInSeconds = 2)

            // play more content, then a second mid-roll
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 8000)
            ssaiIntegration.playAdBreak(timeOffset = 10, adCount = 1, durationInSeconds = 4)

            // play to the end of content, then a post-roll (negative time offset)
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 12000)
            ssaiIntegration.playAdBreak(timeOffset = -1, adCount = 1, durationInSeconds = 3)

            withContext(mainScope.coroutineContext) {
                if (!theoPlayerView.isDestroyed) {
                    theoPlayerView.onDestroy()
                }
            }
            // wait a bit to make sure the last play sample is sent
            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)
            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // one ad per break -> four ad samples in total
            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(5)

            val expectedAdIds =
                listOf("test-ssai-0-0", "test-ssai-5-0", "test-ssai-5-1", "test-ssai-10-0", "test-ssai--1-0")
            adEventDataList.forEachIndexed { index, adSample ->
                assertThat(adSample.started).isEqualTo(1)
                assertThat(adSample.quartile1).isEqualTo(1)
                assertThat(adSample.midpoint).isEqualTo(1)
                assertThat(adSample.quartile3).isEqualTo(1)
                assertThat(adSample.completed).isEqualTo(1)
                assertThat(adSample.adIndex).isEqualTo(index)
                assertThat(adSample.adId).isEqualTo(expectedAdIds[index])
            }

            val preRollAd = adEventDataList.first()
            val midRollAd11 = adEventDataList[1]
            val midRollAd12 = adEventDataList[2]
            val midRollAd22 = adEventDataList[3]
            val postRollAd = adEventDataList[4]

            assertThat(preRollAd.adPosition).isEqualTo("preroll")
            assertThat(preRollAd.adDuration).isEqualTo(5000)
            assertThat(preRollAd.adPodPosition).isEqualTo(0)

            assertThat(midRollAd11.adPosition).isEqualTo("midroll")
            assertThat(midRollAd11.adDuration).isEqualTo(2000)
            assertThat(midRollAd11.adPodPosition).isEqualTo(0)
            assertThat(midRollAd12.adPosition).isEqualTo("midroll")
            assertThat(midRollAd12.adDuration).isEqualTo(2000)
            assertThat(midRollAd12.adPodPosition).isEqualTo(1)

            assertThat(midRollAd22.adPosition).isEqualTo("midroll")
            assertThat(midRollAd22.adDuration).isEqualTo(4000)
            assertThat(midRollAd22.adPodPosition).isEqualTo(0)

            assertThat(postRollAd.adPosition).isEqualTo("postroll")
            assertThat(postRollAd.adDuration).isEqualTo(3000)
            assertThat(postRollAd.adPodPosition).isEqualTo(0)

            // every ad gets its own ad impression, but all reference the same video impression
            assertThat(adEventDataList.map { it.adImpressionId }.toSet()).hasSize(5)
            val impressionId = impression.eventDataList.first().impressionId
            adEventDataList.forEach {
                assertThat(it.videoImpressionId).isEqualTo(impressionId)
            }

            // We start with a preroll ad, thus no samples shouldn't be marked without ad
            val eventDataList = impression.eventDataList
            assertThat(DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)).isEmpty()
            assertThat(DataVerifier.getAllSamplesAfterSsaiAdWithIndex(eventDataList, 0)).isNotEmpty

            // the content event data samples that play during an SSAI ad carry the ad metadata of
            // the ad that is running (set in SsaiService.manipulate): ad == 2 (server-side), the
            // ad id and ad position of the break, and ssaiRelatedSample. The adSystem is null since
            // the custom integration does not report one. The adIndex is only stamped onto the
            // first content sample of each ad and lets us map content samples back to the ad.
            val expectedAdPositions = listOf("preroll", "midroll", "midroll", "midroll", "postroll")
            val adIdToPosition = expectedAdIds.zip(expectedAdPositions).toMap()

            val ssaiContentSamples = eventDataList.filter { it.ad == 2 }
            assertThat(ssaiContentSamples).isNotEmpty
            ssaiContentSamples.forEach { sample ->
                assertThat(sample.adId).isIn(expectedAdIds)
                assertThat(sample.adPosition).isEqualTo(adIdToPosition[sample.adId])
                assertThat(sample.adSystem).isNull()
                assertThat(sample.ssaiRelatedSample).isTrue
            }

            ssaiContentSamples.filter { it.adIndex != null }.forEach { sample ->
                val adIndex = sample.adIndex!!
                assertThat(adIndex).isBetween(0, expectedAdIds.lastIndex)
                assertThat(sample.adId).isEqualTo(expectedAdIds[adIndex])
            }
        }

    @Test
    fun test_customSsai_errorDuringAd_sendsAdErrorSample() =
        runBlockingTest {
            // arrange + act
            attachCollectorAndWaitUntilSsaiReady()
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            // begin an ad, play into it, then raise a fatal error while the ad is still in progress
            ssaiIntegration.playAdBreakWithFatalError(timeOffset = 0)

            // the fatal error stops playback - wait for the error detail sample to be sent
            MockedIngress.waitForErrorDetailSample(timeout = 40.seconds)
            withContext(mainScope.coroutineContext) {
                if (!theoPlayerView.isDestroyed) {
                    theoPlayerView.onDestroy()
                }
            }
            // wait a bit to make sure the last play sample is sent
            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)
            val impression = impressionList.first()

            // the ad that was playing when the error occurred is reported as an SSAI ad sample
            // carrying the error (the SSAI error path runs through the state machine, see
            // DefaultStateMachineListener.onError -> SsaiService.sendAdErrorSample)
            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            val adSample = adEventDataList.first()
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.completed).isEqualTo(0)
            assertThat(adSample.adId).isEqualTo("test-ssai-error-0")
            assertThat(adSample.adDuration).isEqualTo(5000)
            assertThat(adSample.errorCode).isNotNull
            assertThat(adSample.errorMessage).isNotBlank
            assertThat(adSample.errorSeverity).isEqualTo(ErrorSeverity.CRITICAL.toString())

            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)
            val impressionId = impression.eventDataList.first().impressionId
            adEventDataList.forEach {
                assertThat(it.videoImpressionId).isEqualTo(impressionId)
            }

            // the player error is also reported on the regular event data
            val errorSample = impression.eventDataList.first { it.errorCode != null }
            assertThat(errorSample.errorMessage).isNotEmpty
            assertThat(errorSample.errorSeverity).isEqualTo(ErrorSeverity.CRITICAL)

            assertThat(impression.errorDetailList).hasSize(1)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        }

    // FIXME: this testcase needs to be updated once we have proper ad error tracking
    @Test
    fun test_customSsai_adBeaconingError_onlyAffectsAdNotContent() =
        runBlockingTest {
            // arrange + act
            attachCollectorAndWaitUntilSsaiReady()

            // play content first, so the ad break is a mid-roll surrounded by content
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

            // a non-fatal ad error (e.g. ad beaconing/metadata parse failure): the ad raises an
            // error but playback is NOT stopped, so the ad plays out and content continues
            ssaiIntegration.playAdBreakWithAdError(timeOffset = 5)

            // content keeps playing after the errored ad break
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 10000)

            withContext(mainScope.coroutineContext) {
                player.pause()
            }
            // wait a bit to make sure the last play sample is sent
            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList).hasSize(1)
            val impression = impressionList.first()

            // the main content is untouched: no error detail sample and no content error samples
            DataVerifier.verifyHasNoErrorSamples(impression)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            // the SSAI ad played to completion despite the error
            val ssaiAdSample = adEventDataList.first()
            assertThat(ssaiAdSample.started).isEqualTo(1)
            assertThat(ssaiAdSample.completed).isEqualTo(1)
            assertThat(ssaiAdSample.adId).isEqualTo("test-ssai-aderror-5")
            assertThat(ssaiAdSample.adDuration).isEqualTo(5000)
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)
            val impressionId = impression.eventDataList.first().impressionId
            adEventDataList.forEach {
                assertThat(it.videoImpressionId).isEqualTo(impressionId)
            }

            // content was tracked both before and after the errored ad and kept playing
            val eventDataList = impression.eventDataList
            assertThat(DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)).isNotEmpty
            assertThat(DataVerifier.getAllSamplesAfterSsaiAdWithIndex(eventDataList, 0)).isNotEmpty

            val eventDataList1 = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList1)
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(eventDataList1)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList1)
        }

    @Test
    fun test_customSsai_mediaKind_multipleAds_mapEachCustomDataMapOntoItsOwnAdSample() =
        runBlockingTest {
            // mimic the MediaKind custom integration: ads report customIntegration == "mediakind" and
            // carry all their metadata in a map on Ad.customData (rather than on a dedicated ad type).
            ssaiIntegration = TestSsaiAdIntegration(mainScope, integrationId = "mediakind")

            // arrange + act
            attachCollectorAndWaitUntilSsaiReady()

            // let regular content play before the ad break
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 2000)

            // a two-ad pod: a paid ad followed by a slate. Mixed value types and an unknown key verify
            // value stringification and that unknown keys are ignored by MediaKindAdMapper.
            val firstAdCustomData =
                mapOf(
                    "adSystem" to "MediaKind",
                    "creativeId" to "creative-1",
                    "creativeAdId" to "creative-ad-1",
                    "advertiserName" to "ACME Corp",
                    "title" to "MediaKind Ad One",
                    "universalAdIdValue" to "uaid-1",
                    "universalAdIdRegistry" to "ad-id.org",
                    "isSlate" to false,
                    "unknownKey" to "should-be-ignored",
                )
            val secondAdCustomData =
                mapOf(
                    "adSystem" to "MediaKind",
                    "creativeId" to "creative-2",
                    "advertiserName" to "Globex",
                    "title" to "MediaKind Slate",
                    "isSlate" to "true",
                )

            ssaiIntegration.playAdBreakWithCustomData(
                timeOffset = 0,
                customData = listOf(firstAdCustomData, secondAdCustomData),
            )

            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 6000)

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

            // one ad sample per ad in the pod
            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(2)

            val firstAd = adEventDataList[0]
            val secondAd = adEventDataList[1]

            // both ads play through every quartile and share the same pod at increasing positions
            adEventDataList.forEachIndexed { index, adSample ->
                assertThat(adSample.started).isEqualTo(1)
                assertThat(adSample.quartile1).isEqualTo(1)
                assertThat(adSample.midpoint).isEqualTo(1)
                assertThat(adSample.quartile3).isEqualTo(1)
                assertThat(adSample.completed).isEqualTo(1)
                assertThat(adSample.adIndex).isEqualTo(index)
                assertThat(adSample.adPodPosition).isEqualTo(index)
                assertThat(adSample.adPosition).isEqualTo("preroll")
                assertThat(adSample.adSystem).isEqualTo("MediaKind")
                // duration is read off the LinearAd (5s default), reported in milliseconds
                assertThat(adSample.adDuration).isEqualTo(5000)
            }

            // the ad id is intrinsic to the ad (Ad.getId()), not taken from the customData map
            assertThat(firstAd.adId).isEqualTo("test-ssai-customdata-0-0")
            assertThat(secondAd.adId).isEqualTo("test-ssai-customdata-0-1")

            // each ad's own customData map is mapped onto its own sample
            assertThat(firstAd.creativeId).isEqualTo("creative-1")
            assertThat(firstAd.creativeAdId).isEqualTo("creative-ad-1")
            assertThat(firstAd.advertiserName).isEqualTo("ACME Corp")
            assertThat(firstAd.adTitle).isEqualTo("MediaKind Ad One")
            assertThat(firstAd.universalAdIdValue).isEqualTo("uaid-1")
            assertThat(firstAd.universalAdIdRegistry).isEqualTo("ad-id.org")
            assertThat(firstAd.isSlate).isFalse

            assertThat(secondAd.creativeId).isEqualTo("creative-2")
            assertThat(secondAd.advertiserName).isEqualTo("Globex")
            assertThat(secondAd.adTitle).isEqualTo("MediaKind Slate")
            assertThat(secondAd.isSlate).isTrue
            // keys absent from the second ad's map stay null
            assertThat(secondAd.creativeAdId).isNull()
            assertThat(secondAd.universalAdIdValue).isNull()

            // verify basic ad info per ad: the two samples are distinct ads (each with its own
            // adImpressionId), so they can't be checked as a single ad impression in one call.
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(listOf(firstAd))
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(listOf(secondAd))
            assertThat(firstAd.adImpressionId).isNotEqualTo(secondAd.adImpressionId)

            // both ads belong to the same video impression
            val impressionId = impression.eventDataList.first().impressionId
            adEventDataList.forEach {
                assertThat(it.videoImpressionId).isEqualTo(impressionId)
            }
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        }
}
