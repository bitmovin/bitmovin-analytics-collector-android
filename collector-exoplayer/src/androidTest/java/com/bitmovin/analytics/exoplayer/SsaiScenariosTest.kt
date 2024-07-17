package com.bitmovin.analytics.exoplayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdPosition
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using exoplayer player
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh` in the root folder
// Tests use logcat logs to get the sent analytics samples
@RunWith(AndroidJUnit4::class)
class SsaiScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val defaultSample = TestSources.HLS_REDBULL
    private val defaultMediaItem = MediaItem.fromUri(defaultSample.m3u8Url!!)

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private var defaultSourceMetadata: SourceMetadata
        get() =
            SourceMetadata(
                title = metadataGenerator.getTestTitle(),
                customData = CustomData(customData1 = "custom-data-1"),
            )
        set(value) {}

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
    fun tearDown() =
        runBlockingTest {
            MockedIngress.stopServer()
            withContext(mainScope.coroutineContext) {
                player.release()
            }
            // wait a bit for player to be destroyed
            Thread.sleep(100)
        }

    @Test
    fun test_adBreakStart_adStart_adStart_adBreakEnd_sets_right_values() =
        runBlockingTest {
            // arrange
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
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

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-2", "test-ad-system-2", CustomData(customData2 = "ad-test-custom-data-2")),
                )
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
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
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(4)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isEqualTo(0)

            val firstAdSamples = DataVerifier.getSsaiAdSamplesByIndex(eventDataList, 0)
            assertThat(firstAdSamples.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                firstAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1"),
                CustomData(customData1 = "ad-test-custom-data-1"),
                0,
            )

            val secondAdSamples = DataVerifier.getSsaiAdSamplesByIndex(eventDataList, 1)
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
    fun test_ignore_adStart_call_if_adBreakStart_has_not_been_called() =
        runBlockingTest {
            // arrange
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adStart(SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"))
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
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
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(eventDataList)
        }

    @Test
    fun test_ignore_adBreakEnd_call_if_adBreakStart_has_not_been_called() =
        runBlockingTest {
            // arrange
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakEnd()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
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
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(eventDataList)
        }

    @Test
    fun test_no_sample_sent_when_adBreak_was_closed_without_adStart_call_during_adBreak() =
        runBlockingTest {
            // arrange
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
                player.play()
                collector.ssai.adBreakStart(SsaiAdBreakMetadata(SsaiAdPosition.PREROLL))
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakEnd()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
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
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(eventDataList)
        }

    @Test
    fun test_increase_and_set_adIndex_only_on_every_first_ad_sample() =
        runBlockingTest {
            // arrange
            // arrange
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
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

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

            withContext(mainScope.coroutineContext) {
                player.pause()
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-2", "test-ad-system-2", CustomData(customData2 = "ad-test-custom-data-2")),
                )
                player.pause()
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakEnd()
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
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(8)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isEqualTo(0)

            val firstAdSamples = DataVerifier.getSsaiAdSamplesByIndex(eventDataList, 0)
            assertThat(firstAdSamples.size).isGreaterThanOrEqualTo(1)
            DataVerifier.verifyDataForSsaiAdSamples(
                firstAdSamples,
                SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                SsaiAdMetadata("test-ad-id-1", "test-ad-system-1"),
                CustomData(customData1 = "ad-test-custom-data-1"),
                0,
            )

            val secondAdSamples = DataVerifier.getSsaiAdSamplesByIndex(eventDataList, 1)
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
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
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

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakEnd()
                player.pause()
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(
                        SsaiAdPosition.MIDROLL,
                    ),
                )
                collector.ssai.adStart(SsaiAdMetadata("test-ad-id-2", "test-ad-system-2"))
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

            withContext(mainScope.coroutineContext) {
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
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(6)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isEqualTo(0)

            val firstAdSamples = DataVerifier.getSsaiAdSamplesByIndex(eventDataList, 0)
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

            val secondAdSamples = DataVerifier.getSsaiAdSamplesByIndex(eventDataList, 1)
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
            // arrange
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 4000)

            withContext(mainScope.coroutineContext) {
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
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(5)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isGreaterThanOrEqualTo(4)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesBeforeFirstAd)

            val firstAdSamples = DataVerifier.getSsaiAdSamplesByIndex(eventDataList, 0)
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
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
                player.play()
                collector.ssai.adBreakStart(
                    SsaiAdBreakMetadata(SsaiAdPosition.PREROLL),
                )
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

            withContext(mainScope.coroutineContext) {
                player.pause()
                collector.ssai.adStart(
                    SsaiAdMetadata("test-ad-id-1", "test-ad-system-1", CustomData(customData1 = "ad-test-custom-data-1")),
                )
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
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
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(4)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesBeforeFirstAd)
            DataVerifier.verifyCustomData(samplesBeforeFirstAd, defaultSourceMetadata.customData)

            val firstAdSamples = DataVerifier.getSsaiAdSamplesByIndex(eventDataList, 0)
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
            val collector = IExoPlayerCollector.create(appContext, defaultAnalyticsConfig)
            collector.sourceMetadata = defaultSourceMetadata

            // act
            withContext(mainScope.coroutineContext) {
                collector.attachPlayer(player)
                player.setMediaItem(defaultMediaItem)
                player.prepare()
                player.play()
            }

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 1500)

            withContext(mainScope.coroutineContext) {
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

            ExoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 3000)

            withContext(mainScope.coroutineContext) {
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
            assertThat(eventDataList.size).isGreaterThanOrEqualTo(5)

            val samplesBeforeFirstAd = DataVerifier.getSamplesBeforeFirstSsaiAd(eventDataList)
            assertThat(samplesBeforeFirstAd.size).isGreaterThanOrEqualTo(2)
            DataVerifier.verifyHasNoSsaiAdSamples(samplesBeforeFirstAd)

            val firstAdSample = DataVerifier.getSsaiAdSamplesByIndex(eventDataList, 0)
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
}
