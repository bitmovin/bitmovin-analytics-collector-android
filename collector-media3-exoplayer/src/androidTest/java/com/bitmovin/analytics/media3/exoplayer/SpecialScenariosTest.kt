package com.bitmovin.analytics.media3.exoplayer

import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.StreamFormat
import com.bitmovin.analytics.media3.exoplayer.api.IMedia3ExoPlayerCollector
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpecialScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val defaultSample = TestSources.HLS_REDBULL
    private val defaultMediaItem = MediaItem.fromUri(defaultSample.m3u8Url!!)

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private val forceLowestQuality =
        TrackSelectionParameters.Builder(appContext)
            .setForceLowestBitrate(true)
            .build()

    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
    }

    @After
    fun teardown() =
        runBlockingTest {
            MockedIngress.stopServer()
            Thread.sleep(100)
        }

    @Test
    fun test_useOneCollectorFor2DifferentPlayerInstances() =
        runBlockingTest {
            val mediaProgressive = MediaItem.fromUri(TestSources.PROGRESSIVE.progUrl!!)
            val mediaDASH = MediaItem.fromUri(TestSources.DASH.mpdUrl!!)
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            collector.sourceMetadata =
                metadataGenerator.generate(
                    title = metadataGenerator.getTestTitle(),
                    addition = mediaProgressive.toString(),
                )

            val player1 = ExoPlayer.Builder(appContext).build()
            withContext(mainScope.coroutineContext) {
                player1.volume = 0.0f
                collector.attachPlayer(player1)
                player1.setMediaItem(mediaProgressive)
                player1.trackSelectionParameters = forceLowestQuality
                player1.prepare()
                player1.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player1, 500)

            // start second player and attach the same collector
            val player2 = ExoPlayer.Builder(appContext).build()
            withContext(mainScope.coroutineContext) {
                player2.volume = 0.0f
                collector.detachPlayer()
                collector.attachPlayer(player2)
                player2.setMediaItem(mediaDASH)
                player2.trackSelectionParameters = forceLowestQuality
                player2.prepare()
                player2.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player2, 500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player1.release()
                player2.release()
            }

            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(2)

            val progEvents = impressions[0]
            val dashEvents = impressions[1]

            DataVerifier.verifyHasNoErrorSamples(progEvents)
            DataVerifier.verifyHasNoErrorSamples(dashEvents)

            val progStartup = EventDataUtils.getStartupEvent(progEvents.eventDataList)
            val dashStartup = EventDataUtils.getStartupEvent(dashEvents.eventDataList)
            assertStartupSamples(progStartup, dashStartup)
        }

    @Test
    fun test_attachWithoutDetachingFirst() =
        runBlockingTest {
            val mediaProgressive = MediaItem.fromUri(TestSources.PROGRESSIVE.progUrl!!)
            val mediaDASH = MediaItem.fromUri(TestSources.DASH.mpdUrl!!)
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            collector.sourceMetadata =
                metadataGenerator.generate(
                    title = metadataGenerator.getTestTitle(),
                    addition = mediaProgressive.toString(),
                )

            val player1 = ExoPlayer.Builder(appContext).build()
            withContext(mainScope.coroutineContext) {
                player1.volume = 0.0f
                collector.attachPlayer(player1)
                player1.setMediaItem(mediaProgressive)
                player1.trackSelectionParameters = forceLowestQuality
                player1.prepare()
                player1.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player1, 500)

            withContext(mainScope.coroutineContext) {
                player1.setMediaItem(mediaDASH)
                // attach to player1 (for source switch)
                // without detaching first
                collector.attachPlayer(player1)
                player1.prepare()
                player1.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player1, 500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player1.release()
            }

            Thread.sleep(500)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(2)

            val progEvents = impressions[0]
            val dashEvents = impressions[1]

            DataVerifier.verifyHasNoErrorSamples(progEvents)
            DataVerifier.verifyHasNoErrorSamples(dashEvents)

            val progStartup = EventDataUtils.getStartupEvent(progEvents.eventDataList)
            val dashStartup = EventDataUtils.getStartupEvent(dashEvents.eventDataList)
            assertStartupSamples(progStartup, dashStartup)
        }

    @Test
    fun test_reuseCollectorForMultipleSessions_deactivatePlayWhenReadyBeforePreparingSecondMediaItem() =
        runBlockingTest {
            val mediaProgressive = MediaItem.fromUri(TestSources.PROGRESSIVE.progUrl!!)
            val mediaDASH = MediaItem.fromUri(TestSources.DASH.mpdUrl!!)
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            collector.sourceMetadata =
                metadataGenerator.generate(
                    title = metadataGenerator.getTestTitle(),
                    addition = mediaProgressive.toString(),
                )

            val player1 = ExoPlayer.Builder(appContext).build()
            withContext(mainScope.coroutineContext) {
                player1.volume = 0.0f
                collector.attachPlayer(player1)
                player1.setMediaItem(mediaProgressive)
                player1.trackSelectionParameters = forceLowestQuality
                player1.prepare()
                player1.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player1, 500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                // setting a new media while playing, causes implicit autoplay for new media
                // disabling the autoplay right afterwards, is a scenario we want to test here
                // since this seemed to cause issues with how linkedin integrated (AN-4778)
                player1.setMediaItem(mediaDASH)
                collector.attachPlayer(player1)

                // setting playWhenReady to false, to avoid autoplay
                player1.playWhenReady = false
                player1.prepare()
            }

            // Wait for a bit so that we can make sure this
            // is not added to the startuptime (and thus verify that we cannot run into AN-4778)
            Thread.sleep(3000)

            withContext(mainScope.coroutineContext) {
                player1.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player1, 2000)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player1.release()
            }

            Thread.sleep(200)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(2)

            val progEvents = impressions[0]
            val dashEvents = impressions[1]

            DataVerifier.verifyHasNoErrorSamples(progEvents)
            DataVerifier.verifyHasNoErrorSamples(dashEvents)

            val progStartup = EventDataUtils.getStartupEvent(progEvents.eventDataList)
            val dashStartup = EventDataUtils.getStartupEvent(dashEvents.eventDataList)

            // Since we prepare and wait 5 seconds after preparing, startup should be very fast
            assertThat(dashStartup.videoStartupTime).isLessThan(500)
            assertStartupSamples(progStartup, dashStartup)
        }

    @Test
    fun test_reuseCollectorForMultipleSessions_setNewMediaItemWhilePlaying() =
        runBlockingTest {
            val mediaProgressive = MediaItem.fromUri(TestSources.PROGRESSIVE.progUrl!!)
            val mediaDASH = MediaItem.fromUri(TestSources.DASH.mpdUrl!!)
            val collector = IMedia3ExoPlayerCollector.create(appContext, defaultAnalyticsConfig)

            collector.sourceMetadata =
                metadataGenerator.generate(
                    title = metadataGenerator.getTestTitle(),
                    addition = mediaProgressive.toString(),
                )

            val player1 = ExoPlayer.Builder(appContext).build()
            withContext(mainScope.coroutineContext) {
                player1.volume = 0.0f
                collector.attachPlayer(player1)
                player1.setMediaItem(mediaProgressive)
                player1.trackSelectionParameters = forceLowestQuality
                player1.prepare()
                player1.play()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player1, 500)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                // setting a new media while playing, causes implicit autoplay for new media
                player1.setMediaItem(mediaDASH)
                collector.attachPlayer(player1)
                player1.prepare()
            }

            Media3PlayerPlaybackUtils.waitUntilPlayerHasPlayedToMs(player1, 2000)

            withContext(mainScope.coroutineContext) {
                collector.detachPlayer()
                player1.release()
            }

            Thread.sleep(200)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(2)

            val progEvents = impressions[0]
            val dashEvents = impressions[1]

            DataVerifier.verifyHasNoErrorSamples(progEvents)
            DataVerifier.verifyHasNoErrorSamples(dashEvents)

            val progStartup = EventDataUtils.getStartupEvent(progEvents.eventDataList)
            val dashStartup = EventDataUtils.getStartupEvent(dashEvents.eventDataList)

            assertStartupSamples(progStartup, dashStartup)
        }

    private fun assertStartupSamples(
        progStartup: EventData,
        dashStartup: EventData,
    ) {
        assertThat(progStartup.streamFormat).isEqualTo(StreamFormat.PROGRESSIVE.toString().lowercase())
        assertThat(progStartup.progUrl).isEqualTo(TestSources.PROGRESSIVE.progUrl!!.substringBefore("?"))

        assertThat(dashStartup.streamFormat).isEqualTo(StreamFormat.DASH.toString().lowercase())
        assertThat(dashStartup.mpdUrl).isEqualTo(TestSources.DASH.mpdUrl!!.substringBefore("?"))
    }
}
