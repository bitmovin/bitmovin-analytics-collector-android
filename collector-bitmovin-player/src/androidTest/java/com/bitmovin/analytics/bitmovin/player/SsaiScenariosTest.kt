package com.bitmovin.analytics.bitmovin.player
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdPosition
import com.bitmovin.analytics.api.ssai.SsaiAdQuartile
import com.bitmovin.analytics.api.ssai.SsaiAdQuartileMetadata
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.SsaiDataVerifier
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.media.AdaptationConfig
import com.bitmovin.player.api.playlist.PlaylistConfig
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using bitmovin player
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class SsaiScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var defaultPlayer: Player

    private val defaultSample = TestSources.HLS_REDBULL

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private val defaultSourceMetadata: SourceMetadata
        inline get() = metadataGenerator.generate(customData = CustomData(customData1 = "custom-data-1"))

    private val defaultSource = Source(SourceConfig.fromUrl(defaultSample.m3u8Url!!))
    private val defaultMetadata = DefaultMetadata(customUserId = "test-user-id")

    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        // purging database to have a clean state for each test
        EventDatabaseTestHelper.purge(appContext)

        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig =
            TestConfig.createAnalyticsConfig(
                backendUrl = mockedIngressUrl,
                analyticsKey = "ab0544de-d8b7-4a34-8f66-11ad5cb11945",
            )
        val playerConfig =
            PlayerConfig(
                key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                playbackConfig = PlaybackConfig(),
                adaptationConfig = AdaptationConfig(maxSelectableVideoBitrate = 628000),
            )
        defaultPlayer = Player.create(appContext, playerConfig)
    }

    @After
    fun tearDown() =
        runBlockingTest {
            withContext(mainScope.coroutineContext) {
                defaultPlayer.destroy()
            }
            // wait a bit for player to be destroyed
            Thread.sleep(100)
        }

    @Test
    fun test_adBreakStart_adStart_adStart_adBreakEnd_sets_right_values() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            val exepctedCustomDataOnFirstSample =
                CustomData(
                    customData1 = "ad-test-custom-data-1",
                    customData30 = "ad-test-custom-data-30",
                    customData50 = "ad-test-custom-data-50",
                )

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )
                collector.ssai.adStart(
                    SsaiAdMetadata(
                        "test-ad-id-1",
                        "test-ad-system-1",
                        exepctedCustomDataOnFirstSample,
                    ),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-2", "test-ad-system-2", CustomData(customData2 = "ad-test-custom-data-2")),
                )
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakEnd()
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(4)

            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(eventDataList)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isEqualTo(0)

            val firstAdSamples = DataVerifier.getSsaiSamplesByIndex(eventDataList, 0)
            assertThat(firstAdSamples.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                firstAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1"),
                exepctedCustomDataOnFirstSample,
                0,
            )

            val firstSsaiAdSample = SsaiDataVerifier.getSsaiAdEventSampleByAdIndex(impression.adEventDataList, 0)
            SsaiDataVerifier.verifyCustomData(firstSsaiAdSample, exepctedCustomDataOnFirstSample)

            val secondAdSamples = DataVerifier.getSsaiSamplesByIndex(eventDataList, 1)
            assertThat(secondAdSamples.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                secondAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"),
                CustomData(customData1 = defaultSourceMetadata.customData.customData1, customData2 = "ad-test-custom-data-2"),
                1,
            )

            val samplesAfterEndAdBreak = DataVerifier.getAllSamplesAfterSsaiAdWithIndex(eventDataList, 1)
            assertThat(samplesAfterEndAdBreak.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesAfterEndAdBreak)
            DataVerifier.verifyCustomData(samplesAfterEndAdBreak, defaultSourceMetadata.customData)
        }

    @Test
    fun test_adEngagementMetrics_track_all_quartiles() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.THIRD)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.COMPLETED)
                collector.ssai.adBreakEnd()
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            val adSample = adEventDataList[0]
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.quartile1).isEqualTo(1)
            assertThat(adSample.midpoint).isEqualTo(1)
            assertThat(adSample.quartile3).isEqualTo(1)
            assertThat(adSample.completed).isEqualTo(1)

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(adEventDataList, 0)
            SsaiDataVerifier.verifySamplesHaveSameAdSystem(adEventDataList, "test-ad-system-1")
            SsaiDataVerifier.verifySamplesHaveSameAdId(adEventDataList, "test-ad-id-1")
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)
        }

    @Test
    fun test_adEngagementMetricsDisabled_DO_NOT_track_all_quartiles() =
        runBlockingTest {
            // arrange
            val configWithSsaiEngagementTrackingDisabled =
                AnalyticsConfig(
                    licenseKey = defaultAnalyticsConfig.licenseKey,
                    backendUrl = mockedIngressUrl,
                )
            val collector = IBitmovinPlayerCollector.create(appContext, configWithSsaiEngagementTrackingDisabled, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.THIRD)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.COMPLETED)
                collector.ssai.adBreakEnd()
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(0)
        }

    @Test
    fun test_adEngagementMetrics_track_all_quartiles_for_2_ads() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.THIRD)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.COMPLETED)
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-2", "test-ad-system-2", CustomData(customData2 = "ad-test-custom-data-2")),
                )
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 5000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 5500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.THIRD)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 6000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.COMPLETED)
                collector.ssai.adBreakEnd()
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(2)

            val firstAdSample = adEventDataList[0]
            assertThat(firstAdSample.started).isEqualTo(1)
            assertThat(firstAdSample.quartile1).isEqualTo(1)
            assertThat(firstAdSample.midpoint).isEqualTo(1)
            assertThat(firstAdSample.quartile3).isEqualTo(1)
            assertThat(firstAdSample.completed).isEqualTo(1)

            // verify that delta is set close to the time
            assertThat(firstAdSample.timeSinceAdStartedInMs).isBetween(3500, 5500)

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(listOf(firstAdSample), 0)
            SsaiDataVerifier.verifySamplesHaveSameAdSystem(listOf(firstAdSample), "test-ad-system-1")
            SsaiDataVerifier.verifySamplesHaveSameAdId(listOf(firstAdSample), "test-ad-id-1")
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(listOf(firstAdSample))

            val secondAdSample = adEventDataList[1]
            assertThat(secondAdSample.started).isEqualTo(1)
            assertThat(secondAdSample.quartile1).isEqualTo(1)
            assertThat(secondAdSample.midpoint).isEqualTo(1)
            assertThat(secondAdSample.quartile3).isEqualTo(1)
            assertThat(secondAdSample.completed).isEqualTo(1)

            assertThat(secondAdSample.timeSinceAdStartedInMs).isBetween(1500, 3000)

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(listOf(secondAdSample), 1)
            SsaiDataVerifier.verifySamplesHaveSameAdSystem(listOf(secondAdSample), "test-ad-system-2")
            SsaiDataVerifier.verifySamplesHaveSameAdId(listOf(secondAdSample), "test-ad-id-2")
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(listOf(secondAdSample))

            assertThat(firstAdSample.adImpressionId).isNotEqualTo(secondAdSample.adImpressionId)
            assertThat(firstAdSample.videoImpressionId).isEqualTo(secondAdSample.videoImpressionId)
        }

    @Test
    fun test_adEngagementMetrics_track_error() =
        runBlockingTest {
            val corruptedStream = Samples.CORRUPT_DASH
            val corruptedStreamSource = Source(SourceConfig.fromUrl(corruptedStream.uri.toString()))
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(corruptedStreamSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(corruptedStreamSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                collector.ssai.adStart(
                    SsaiAdMetadata(
                        "test-ad-id-1",
                        "test-ad-system-1",
                        CustomData(customData1 = "ad-test-custom-data-1"),
                    ),
                )
                defaultPlayer.play()
            }

            // wait a bit to make sure we catch the error
            Thread.sleep(3000)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            val adSampleWithError = adEventDataList[0]
            assertThat(adSampleWithError.started).isEqualTo(1)
            assertThat(adSampleWithError.errorCode).isNotNull()
            assertThat(adSampleWithError.errorMessage).isNotNull()
        }

    @Test
    fun test_adEngagementMetricsDisabled_do_NOT_track_error() =
        runBlockingTest {
            val corruptedStream = Samples.CORRUPT_DASH
            val corruptedStreamSource = Source(SourceConfig.fromUrl(corruptedStream.uri.toString()))
            // arrange
            val configWithSsaiEngagementTrackingDisabled =
                AnalyticsConfig(
                    licenseKey = defaultAnalyticsConfig.licenseKey,
                    backendUrl = mockedIngressUrl,
                )
            val collector = IBitmovinPlayerCollector.create(appContext, configWithSsaiEngagementTrackingDisabled, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(corruptedStreamSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(corruptedStreamSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                collector.ssai.adStart(
                    SsaiAdMetadata(
                        "test-ad-id-1",
                        "test-ad-system-1",
                        CustomData(customData1 = "ad-test-custom-data-1"),
                    ),
                )
                defaultPlayer.play()
            }

            // wait a bit to make sure we catch the error
            Thread.sleep(3000)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(0)
        }

    @Test
    fun test_adEngagementMetrics_multiple_quartile_calls_are_debounced() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
                collector.ssai.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
                collector.ssai.adQuartileFinished(SsaiAdQuartile.THIRD)
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.COMPLETED)
                collector.ssai.adQuartileFinished(SsaiAdQuartile.THIRD)
                collector.ssai.adBreakEnd()
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

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

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(adEventDataList, 0)
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)
        }

    @Test
    fun test_adEngagementMetrics_failedBeaconUrlIsReported() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                val adQuartileMetadata = SsaiAdQuartileMetadata("https://failed-beacon-url.com")
                collector.ssai.adQuartileFinished(SsaiAdQuartile.MIDPOINT, adQuartileMetadata)
                collector.ssai.adBreakEnd()
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val adEventDataList = impression.adEventDataList
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)
            assertThat(adEventDataList).hasSize(1)

            val adSample = adEventDataList[0]
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.quartile1).isEqualTo(1)
            assertThat(adSample.midpoint).isEqualTo(1)
            assertThat(adSample.quartile3).isEqualTo(0)
            assertThat(adSample.completed).isEqualTo(0)
            assertThat(adSample.midpointFailedBeaconUrl).isEqualTo("https://failed-beacon-url.com")

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(adEventDataList, 0)
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)
        }

    @Test
    fun test_ignore_adStart_call_if_adBreakStart_has_not_been_called() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adStart(SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"))
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(eventDataList)
        }

    @Test
    fun test_ignore_adBreakEnd_call_if_adBreakStart_has_not_been_called() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakEnd()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(eventDataList)
        }

    @Test
    fun test_no_sample_sent_when_adBreak_was_closed_without_adStart_call_during_adBreak() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                defaultPlayer.play()
                collector.ssai.adBreakStart(SsaiAdBreakMetadata(SsaiAdPosition.PREROLL))
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakEnd()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(eventDataList)
        }

    @Test
    fun test_increase_and_set_adIndex_only_on_every_first_ad_sample() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(
                        SsaiAdPosition.PREROLL,
                    ),
                )
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-2", "test-ad-system-2", CustomData(customData2 = "ad-test-custom-data-2")),
                )
                defaultPlayer.pause()
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakEnd()
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(8)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isEqualTo(0)

            val firstAdSamples = DataVerifier.getSsaiSamplesByIndex(eventDataList, 0)
            assertThat(firstAdSamples.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                firstAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1"),
                CustomData(customData1 = "ad-test-custom-data-1"),
                0,
            )

            val secondAdSamples = DataVerifier.getSsaiSamplesByIndex(eventDataList, 1)
            assertThat(secondAdSamples.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                secondAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"),
                CustomData(customData1 = defaultSourceMetadata.customData.customData1, customData2 = "ad-test-custom-data-2"),
                1,
            )

            val samplesAfterEndAdBreak = DataVerifier.getAllSamplesAfterSsaiAdWithIndex(eventDataList, 1)
            assertThat(samplesAfterEndAdBreak.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesAfterEndAdBreak)
            DataVerifier.verifyCustomData(samplesAfterEndAdBreak, defaultSourceMetadata.customData)
        }

    @Test
    fun test_do_not_reset_adIndex_between_adBreaks() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
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
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakEnd()
                defaultPlayer.pause()
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(
                        SsaiAdPosition.MIDROLL,
                    ),
                )
                collector.ssai.adStart(SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"))
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(6)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isEqualTo(0)

            val firstAdSamples = DataVerifier.getSsaiSamplesByIndex(eventDataList, 0)
            assertThat(firstAdSamples.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                firstAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1"),
                CustomData(customData1 = "ad-test-custom-data-1"),
                0,
            )

            val samplesBetweenAds = DataVerifier.getSamplesBetweenAds(eventDataList, 0)
            assertThat(samplesBetweenAds.size).isGreaterThanOrEqualTo(3)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesBetweenAds)
            DataVerifier.verifyCustomData(samplesBetweenAds, defaultSourceMetadata.customData)

            val secondAdSamples = DataVerifier.getSsaiSamplesByIndex(eventDataList, 1)
            assertThat(secondAdSamples.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                secondAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.MIDROLL),
                SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"),
                CustomData(customData1 = defaultSourceMetadata.customData.customData1),
                1,
            )
        }

    @Test
    fun test_does_not_ignore_adBreakStart_when_player_is_paused() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(5)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isGreaterThanOrEqualTo(4)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesBeforeFirstAd)

            val firstAdSamples = DataVerifier.getSsaiSamplesByIndex(eventDataList, 0)
            assertThat(firstAdSamples.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                firstAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1"),
                CustomData(customData1 = "ad-test-custom-data-1"),
                0,
            )
        }

    @Test
    fun test_does_not_send_sample_but_sets_metadata_when_adStart_called_with_player_paused() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                defaultPlayer.play()
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(4)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesBeforeFirstAd)
            DataVerifier.verifyCustomData(samplesBeforeFirstAd, defaultSourceMetadata.customData)

            val firstAdSamples = DataVerifier.getSsaiSamplesByIndex(eventDataList, 0)
            assertThat(firstAdSamples.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyDataForSsaiAdSamples(
                firstAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1"),
                CustomData(customData1 = "ad-test-custom-data-1"),
                0,
            )
        }

    @Test
    fun test_does_not_send_sample_but_resets_ssai_related_data_when_adBreakEnd_called_with_player_paused() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.pause()
                collector.ssai.adBreakEnd()
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(5)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesBeforeFirstAd)

            val firstAdSample = DataVerifier.getSsaiSamplesByIndex(eventDataList, 0)
            assertThat(firstAdSample.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                firstAdSample,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1"),
                CustomData(customData1 = "ad-test-custom-data-1"),
                0,
            )

            val samplesAfterFirstAd = DataVerifier.getAllSamplesAfterSsaiAdWithIndex(eventDataList, 0)
            assertThat(samplesAfterFirstAd.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesAfterFirstAd)
            DataVerifier.verifyCustomData(samplesAfterFirstAd, defaultSourceMetadata.customData)
        }

    @Test
    fun test_reset_with_playlist_transition() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)
            val secondSource = Source(SourceConfig.fromUrl(TestSources.DASH_SINTEL_WITH_SUBTITLES.mpdUrl!!))
            val playlistConfig = PlaylistConfig(listOf(defaultSource, secondSource))

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(playlistConfig)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1500)

            withContext(mainScope.coroutineContext) {
                // prevents quality changes for the second source
                defaultPlayer.config.adaptationConfig.maxSelectableVideoBitrate = 258157
                // triggers a playlist transition - resulting in a new impression
                defaultPlayer.playlist.seek(secondSource, 10.0)
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 4000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.MIDROLL),
                )
                collector.ssai.adStart(SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"))
                defaultPlayer.pause()
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(2)

            // First Impression
            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList1 = impression.eventDataList
            assertThat(eventDataList1.size).isGreaterThanOrEqualTo(3)
            DataVerifier.verifyDataForSsaiAdSamples(
                eventDataList1,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1"),
                CustomData(customData1 = "ad-test-custom-data-1"),
                0,
            )

            // Second Impression
            val impression2 = impressionList[1]
            DataVerifier.verifyHasNoErrorSamples(impression2)

            val eventDataList2 = impression2.eventDataList
            assertThat(eventDataList2.size).isGreaterThanOrEqualTo(3)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList2)
            assertThat(samplesBeforeFirstAd.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesBeforeFirstAd)
            DataVerifier.verifyCustomData(samplesBeforeFirstAd, CustomData())

            val firstAdSamples = DataVerifier.getSsaiSamplesByIndex(eventDataList2, 0)
            assertThat(firstAdSamples.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                firstAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.MIDROLL),
                SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"),
                CustomData(),
                0,
            )
        }

    @Test
    fun test_adEngagementMetrics_send_adEngagementData_on_detach() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
                collector.detachPlayer()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            val adSample = adEventDataList[0]
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.quartile1).isEqualTo(1)
            assertThat(adSample.midpoint).isEqualTo(1)
            assertThat(adSample.quartile3).isEqualTo(0)
            assertThat(adSample.completed).isEqualTo(0)

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(adEventDataList, 0)
            SsaiDataVerifier.verifySamplesHaveSameAdSystem(adEventDataList, "test-ad-system-1")
            SsaiDataVerifier.verifySamplesHaveSameAdId(adEventDataList, "test-ad-id-1")
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)
        }

    @Test
    fun test_adEngagementMetrics_send_adEngagementData_on_player_destroy() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
                defaultPlayer.destroy()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            val adSample = adEventDataList[0]
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.quartile1).isEqualTo(1)
            assertThat(adSample.midpoint).isEqualTo(1)
            assertThat(adSample.quartile3).isEqualTo(0)
            assertThat(adSample.completed).isEqualTo(0)

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(adEventDataList, 0)
            SsaiDataVerifier.verifySamplesHaveSameAdSystem(adEventDataList, "test-ad-system-1")
            SsaiDataVerifier.verifySamplesHaveSameAdId(adEventDataList, "test-ad-id-1")
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)
        }

    @Test
    fun test_adEngagementMetrics_send_adEngagementData_on_source_unload() =
        runBlockingTest {
            // arrange
            val collector = IBitmovinPlayerCollector.create(appContext, defaultAnalyticsConfig, defaultMetadata)

            // act
            withContext(mainScope.coroutineContext) {
                collector.setSourceMetadata(defaultSource, defaultSourceMetadata)
                collector.attachPlayer(defaultPlayer)
                defaultPlayer.load(defaultSource)
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )

                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                defaultPlayer.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 1000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.FIRST)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 2000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adQuartileFinished(SsaiAdQuartile.MIDPOINT)
                defaultPlayer.unload()
            }

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(impression.eventDataList)

            val adEventDataList = impression.adEventDataList
            assertThat(adEventDataList).hasSize(1)

            val adSample = adEventDataList[0]
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.quartile1).isEqualTo(1)
            assertThat(adSample.midpoint).isEqualTo(1)
            assertThat(adSample.quartile3).isEqualTo(0)
            assertThat(adSample.completed).isEqualTo(0)

            SsaiDataVerifier.verifySamplesHaveSameAdIndex(adEventDataList, 0)
            SsaiDataVerifier.verifySamplesHaveSameAdSystem(adEventDataList, "test-ad-system-1")
            SsaiDataVerifier.verifySamplesHaveSameAdId(adEventDataList, "test-ad-id-1")
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(adEventDataList)

            val expectedSsaiCustomData =
                CustomData(
                    customData1 = "ad-test-custom-data-1",
                )
            SsaiDataVerifier.verifyCustomData(adEventDataList, expectedSsaiCustomData)
        }
}
