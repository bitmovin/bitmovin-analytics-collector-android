package com.bitmovin.analytics.bitmovin.player
import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.AdvertisingConfig
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdScenariosTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private var defaultSample = TestSources.HLS_REDBULL
    private var defaultSource = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!))

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    private lateinit var mockedIngressUrl: String

    @Before
    fun setup() {
        mockedIngressUrl = MockedIngress.startServer()
        // purging database to have a clean state for each test
        EventDatabaseTestHelper.purge(appContext)
    }

    @Test
    fun test_vodWithAds_playWithAutoplayAndMuted() =
        runBlockingTest {
            // arrange
            // for some reason IMA tags do not work with the gradle managed devices, thus using progressive ads here
            val adSource = AdSource(AdSourceType.Progressive, "https://bitmovin-a.akamaihd.net/content/testing/ads/testad2s.mp4")
            val preRoll = AdItem("pre", adSource)
            // play midroll after 6 seconds
            val midRoll = AdItem("6", adSource)
            val advertisingConfig = AdvertisingConfig(preRoll, midRoll)

            val collector = IBitmovinPlayerCollector.create(appContext, TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl))
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
            Assertions.assertThat(impressionList.size).isEqualTo(1)

            val impression = impressionList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            // we expect 2 adEventData to be sent
            Assertions.assertThat(impression.adEventDataList.size).isEqualTo(2)
            val eventDataWithAdState = impression.eventDataList.filter { x -> x.ad == 1 }
            Assertions.assertThat(eventDataWithAdState.size).isEqualTo(2)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyStaticData(eventDataList, sourceMetadata, defaultSample, BitmovinPlayerConstants.playerInfo)

            // startup sample is second sample (since order of events in player changed in 3.40.0
            DataVerifier.verifyStartupSample(eventData = eventDataList[1], expectedSequenceNumber = 1)

            // TODO: we are not collecting videoStart and videoEnd times correctly when ads are played
            // DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyInvariants(eventDataList)

            val filteredList = EventDataUtils.filterNonDeterministicEvents(eventDataList)
            DataVerifier.verifyThereWasAtLeastOnePlayingSample(filteredList)
        }
}
