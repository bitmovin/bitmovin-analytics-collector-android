package com.bitmovin.analytics.bitmovin.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
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
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// AdTests currently don't work with the managed devices and need to be run with the normal emulator
@RunWith(AndroidJUnit4::class)
class AdScenariosTest {

    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var defaultPlayer: Player

    private var defaultSample = TestSources.HLS_REDBULL
    private var defaultSource = Source.create(SourceConfig.fromUrl(defaultSample.m3u8Url!!))

    @Before
    fun setup() {
        // purging database to have a clean state for each test
        EventDatabaseTestHelper.purge(appContext)

        // logging to mark new test run for logparsing
        LogParser.startTracking()
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = PlaybackConfig())
        defaultPlayer = Player.create(appContext, playerConfig)
    }

    @Test
    fun test_vodWithAds_playWithAutoplayAndMuted() {
        // arrange
        // https://developers.google.com/interactive-media-ads/docs/sdks/android/client-side/tags
        val imaTag = "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct%3Dskippablelinear&correlator="
        val adSource = AdSource(AdSourceType.Ima, imaTag)

        // Setup a pre-roll ad
        val preRoll = AdItem("pre", adSource)
        // play midroll after 3seconds
        val midRoll = AdItem("3", adSource)
        val advertisingConfig = AdvertisingConfig(preRoll, midRoll)

        val collector = IBitmovinPlayerCollector.create(appContext, TestConfig.createAnalyticsConfig())
        val playbackConfig = PlaybackConfig(isAutoplayEnabled = true, isMuted = true)
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = playbackConfig, advertisingConfig = advertisingConfig)
        val localPlayer = Player.create(appContext, playerConfig)
        val sourceMetadata = SourceMetadata(title = "adTest")

        // act
        mainScope.launch {
            collector.attachPlayer(localPlayer)
            collector.setSourceMetadata(defaultSource, sourceMetadata)
            localPlayer.load(defaultSource)
        }

        // wait until midRoll ad is played
        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(localPlayer, 8000)

        mainScope.launch {
            localPlayer.pause()
        }

        // wait a bit to make sure last play sample is sent
        Thread.sleep(500)

        mainScope.launch {
            collector.detachPlayer()
        }

        // assert
        val impressionList = LogParser.extractImpressions()
        Assertions.assertThat(impressionList.size).isEqualTo(1)

        val impression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(impression)

        // we expect 2 adEventData to be sent
        Assertions.assertThat(impression.adEventDataList.size).isEqualTo(2)
        val eventDataWithAdState = impression.eventDataList.filter { x -> x.ad == 1 }
        Assertions.assertThat(eventDataWithAdState.size).isEqualTo(2)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, sourceMetadata, defaultSample, BitmovinPlayerConstants.playerInfo)

        // TODO: verify if this could cause issues
        // startup sample is second sample (since order of events in player changed in 3.40.0
        DataVerifier.verifyStartupSample(eventData = eventDataList[1], expectedSequenceNumber = 1)
        DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
        DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
        DataVerifier.verifyInvariants(eventDataList)

        EventDataUtils.filterNonDeterministicEvents(eventDataList)
        DataVerifier.verifyThereWasAtLeastOnePlayingSample(eventDataList)
        // verify that no other states than startup, playing and ad were reached
        Assertions.assertThat(eventDataList.filter { x -> x.state != "startup" && x.state != "playing" && x.state != "ad" }.size)
            .isEqualTo(0)
    }
}
