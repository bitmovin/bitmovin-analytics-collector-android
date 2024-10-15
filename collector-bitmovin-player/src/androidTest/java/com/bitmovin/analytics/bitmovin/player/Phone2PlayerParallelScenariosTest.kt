package com.bitmovin.analytics.bitmovin.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.analytics.create
import com.bitmovin.player.api.source.SourceBuilder
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phone2PlayerParallelScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private lateinit var player1: Player
    private lateinit var player2: Player

    private lateinit var mockedIngressUrl: String

    private val player1SourceMetadata = SourceMetadata("video1")
    private val player2SourceMetadata = SourceMetadata("video2")

    private val sample1 = TestSources.HLS_REDBULL
    private val sample2 = TestSources.DASH
    private val source1 = SourceBuilder(SourceConfig.fromUrl(sample1.m3u8Url!!)).configureAnalytics(player1SourceMetadata).build()
    private val source2 = SourceBuilder(SourceConfig.fromUrl(sample2.mpdUrl!!)).configureAnalytics(player2SourceMetadata).build()

    @Before
    fun setup() =
        runBlockingTest {
            // purging database to have a clean state for each test
            EventDatabaseTestHelper.purge(appContext)
            mockedIngressUrl = MockedIngress.startServer()
        }

    @Test
    fun test_2_player_in_parallel() =
        runBlockingTest {
            val player1AnalyticsConfig =
                TestConfig.createAnalyticsConfig(
                    backendUrl = mockedIngressUrl,
                    ssaiEngagementTrackingEnabled = false,
                )
            val player2AnalyticsConfig =
                TestConfig.createAnalyticsConfig(
                    backendUrl = mockedIngressUrl,
                    ssaiEngagementTrackingEnabled = false,
                )

            val player1Config =
                PlayerConfig(
                    key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                    playbackConfig = PlaybackConfig(isAutoplayEnabled = true),
                )
            val player2Config =
                PlayerConfig(
                    key = "a6e31908-550a-4f75-b4bc-a9d89880a733",
                    playbackConfig = PlaybackConfig(isAutoplayEnabled = true),
                )

            withContext(mainScope.coroutineContext) {
                player1 = Player.create(appContext, player1Config, player1AnalyticsConfig)
                player2 = Player.create(appContext, player2Config, player2AnalyticsConfig)
                player1.load(source1)
                player2.load(source2)
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player1, 1000)
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player2, 1000)

            withContext(mainScope.coroutineContext) {
                player1.pause()
                player2.pause()
                player1.play()
                player2.play()
            }

            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player1, 4000)
            BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(player2, 4000)

            withContext(mainScope.coroutineContext) {
                player1.destroy()
                player2.destroy()
            }

            Thread.sleep(500)

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionList.size).isEqualTo(2)

            val player1Impression =
                impressionList.first {
                    it.eventDataList[0].videoTitle == player1SourceMetadata.title
                }

            val player2Impression =
                impressionList.first {
                    it.eventDataList[0].videoTitle == player2SourceMetadata.title
                }

            DataVerifier.verifyHasNoErrorSamples(player1Impression)
            DataVerifier.verifyHasNoErrorSamples(player2Impression)

            DataVerifier.verifyStaticData(
                player1Impression.eventDataList,
                player1SourceMetadata,
                sample1,
                BitmovinPlayerConstants.playerInfo,
            )
            DataVerifier.verifyStaticData(
                player2Impression.eventDataList,
                player2SourceMetadata,
                sample2,
                BitmovinPlayerConstants.playerInfo,
            )

            DataVerifier.verifyStartupSample(player1Impression.eventDataList[0])
            DataVerifier.verifyStartupSample(player2Impression.eventDataList[0])

            DataVerifier.verifyExactlyOnePauseSample(player1Impression.eventDataList)
            DataVerifier.verifyExactlyOnePauseSample(player2Impression.eventDataList)
        }
}
