package com.bitmovin.analytics.theoplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.test.utils.DataVerifier
import com.bitmovin.analytics.test.utils.MetadataUtils
import com.bitmovin.analytics.test.utils.MockedIngress
import com.bitmovin.analytics.test.utils.PlaybackUtils
import com.bitmovin.analytics.test.utils.SsaiDataVerifier
import com.bitmovin.analytics.test.utils.TestConfig
import com.bitmovin.analytics.test.utils.runBlockingTest
import com.bitmovin.analytics.theoplayer.api.ITHEOplayerCollector
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.ads.ima.GoogleImaIntegrationFactory
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.GoogleDaiTypedSource
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.ssai.dai.GoogleDaiVodConfiguration
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SsaiScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private var defaultSourceMetadata: SourceMetadata
        get() =
            SourceMetadata(
                title = metadataGenerator.getTestTitle(),
                videoId = "ssaiTest",
                path = "testPath",
            )
        set(_) {}

    // Google's public DAI VOD sample ("tears-of-steel"), as used in THEOplayer's own docs.
    private val daiContentSourceID = "2548831"
    private val daiVideoID = "tears-of-steel"

    private val defaultMetadata = DefaultMetadata(cdnProvider = "cdnProvider", customUserId = "customUserId1")

    private lateinit var player: Player
    private lateinit var theoPlayerView: THEOplayerView
    private lateinit var mockedIngressUrl: String

    private val daiConfig =
        GoogleDaiVodConfiguration.Builder(
            // apiKey, contentSourceID, videoID
            "",
            daiContentSourceID,
            daiVideoID,
        ).build()
    private val daiSource =
        GoogleDaiTypedSource.Builder(daiConfig)
            .build()
    private val googleDaiSourceDescription =
        SourceDescription
            .Builder(daiSource)
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
                // Google DAI is delivered through the Google IMA integration, which must be
                // explicitly registered for DAI sources to work.
                val imaIntegration = GoogleImaIntegrationFactory.createGoogleImaIntegration(theoPlayerView)
                player.addIntegration(imaIntegration)
                player.useLowestRendition()
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
    fun test_vodWithGoogleDaiPreRollAd_playWithAutoplay() =
        runBlockingTest {
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)

            // act
            withContext(mainScope.coroutineContext) {
                player.isAutoplay = true
                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig, defaultMetadata)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = googleDaiSourceDescription
            }

            // wait for the DAI pre-roll ad to start and finish, then let content play
            PlaybackUtils.waitUntil("pre-roll ad started") { player.ads.isPlaying }
            PlaybackUtils.waitUntil("pre-roll ad finished") { !player.ads.isPlaying }
            TheoPlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player, 15000)

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

            // the DAI pre-roll is server-side inserted, so it is tracked as an SSAI ad sample
            assertThat(impression.adEventDataList).isNotEmpty
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(impression.adEventDataList)

            // all ad samples must reference the same video impression
            val impressionId = impression.eventDataList.first().impressionId
            impression.adEventDataList.forEach {
                assertThat(it.videoImpressionId).isEqualTo(impressionId)
            }

            // event data is linked to the SSAI ad and carries the routing header
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        }

    @Test
    fun test_vodWithGoogleDaiPreRollAd_abandonDuringAd() =
        runBlockingTest {
            // arrange
            val analyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
            // act
            withContext(mainScope.coroutineContext) {
                val collector = ITHEOplayerCollector.create(appContext, analyticsConfig, defaultMetadata)
                collector.sourceMetadata = defaultSourceMetadata
                collector.attachPlayer(player)
                player.source = googleDaiSourceDescription
                player.play()
            }

            // wait for the DAI pre-roll ad to start and finish, then let content play
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
            assertThat(impression.adEventDataList).hasSize(1)
            SsaiDataVerifier.verifySamplesHaveBasicAdInfoSet(impression.adEventDataList)

            val adSample = impression.adEventDataList.first()

            // all ad samples must reference the same video impression
            val impressionId = impression.eventDataList.first().impressionId
            assertThat(adSample.videoImpressionId).isEqualTo(impressionId)
            assertThat(adSample.started).isEqualTo(1)
            assertThat(adSample.completed).isEqualTo(0)
            assertThat(adSample.exitedAdBreak).isTrue

            // event data is linked to the SSAI ad and carries the routing header
            val eventDataList = impression.eventDataList
            DataVerifier.verifyInvariants(eventDataList)
            SsaiDataVerifier.verifySsaiRelatedSamplesHaveHeaderSet(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        }
}
