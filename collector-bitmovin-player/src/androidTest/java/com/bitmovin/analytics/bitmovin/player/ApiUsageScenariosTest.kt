package com.bitmovin.analytics.bitmovin.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.persistence.EventDatabaseTestHelper
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.utils.ApiV3Utils
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApiUsageScenariosTest {

    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var defaultPlayer: Player

    private val source1 = Source.create(SourceConfig.fromUrl(TestSources.HLS_REDBULL.m3u8Url!!))
    private val sourceMetadata1 = SourceMetadata(
        title = "hls_redbull",
        videoId = "hls_redbull",
        path = "hls_redbull_path",
        m3u8Url = TestSources.HLS_REDBULL.m3u8Url,
        customData = TestConfig.createDummyCustomData("metadatahls_"),
        cdnProvider = "cdn_provider_hls",
    )

    private val source2 = Source.create(SourceConfig.fromUrl(TestSources.DASH.mpdUrl!!))
    private val sourceMetadata2 = SourceMetadata(
        title = "dashTitle",
        videoId = "dashId",
        path = "dash_path_1",
        mpdUrl = TestSources.DASH.mpdUrl,
        customData = TestConfig.createDummyCustomData("metadatadash_"),
        cdnProvider = "cdn_provider_dash",
    )

    private val sourceMetadata2_2 = SourceMetadata(
        title = "dashTitle2",
        videoId = "dashId2",
        path = "dash_path_2",
        mpdUrl = TestSources.DASH.mpdUrl,
        customData = TestConfig.createDummyCustomData("metadatadash2_"),
        cdnProvider = "cdn_provider_dash2",
    )

    private val source3 = Source.create(SourceConfig.fromUrl(TestSources.PROGRESSIVE.progUrl!!))
    private val sourceMetadata3 = SourceMetadata(
        title = "progTitle",
        videoId = "progId",
        path = "prog_path_1",
        progUrl = TestSources.PROGRESSIVE.progUrl,
        customData = TestConfig.createDummyCustomData("metadataprod_"),
        cdnProvider = "cdn_provider_prod",
    )

    @Before
    fun setup() {
        // purging database to have a clean state for each test
        EventDatabaseTestHelper.purge(appContext)

        // logging to mark new test run for logparsing
        LogParser.startTracking()
        val playerConfig = PlayerConfig(key = "a6e31908-550a-4f75-b4bc-a9d89880a733", playbackConfig = PlaybackConfig())
        defaultPlayer = Player.create(appContext, playerConfig)
    }

    @After
    fun tearDown() {
        mainScope.launch {
            defaultPlayer.destroy()
        }
        // wait a bit for player to be destroyed
        Thread.sleep(100)
    }

    @Test
    fun playlistAndNonPlaylistApiMixed1() {
        val collector = IBitmovinPlayerCollector.create(appContext, TestConfig.createAnalyticsConfig())
        collector.setSourceMetadata(sourceMetadata1, source1)
        collector.setSourceMetadata(sourceMetadata2, source2)

        // calling setCurrentSourceMetadata before source is loaded means
        // that it cannot determine the currently active source
        // thus we expect that the sourceMetadata is stored as generic sourceMetadata that is
        // used when the currently active doesn't have a specific sourceMetadata
        collector.setCurrentSourceMetadata(sourceMetadata3)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(source2)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.load(source3)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            collector.detachPlayer()
        }

        Thread.sleep(300)

        // assert
        val impressionList = LogParser.extractImpressions()
        Assertions.assertThat(impressionList).hasSize(2)

        val firstImpression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(firstImpression)

        DataVerifier.verifyStaticData(firstImpression.eventDataList, sourceMetadata2, TestSources.DASH, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(firstImpression.eventDataList[0])

        val secondImpression = impressionList[1]
        DataVerifier.verifyHasNoErrorSamples(secondImpression)

        DataVerifier.verifyStaticData(secondImpression.eventDataList, sourceMetadata3, TestSources.PROGRESSIVE, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(secondImpression.eventDataList[0], isFirstImpression = false)
    }

    @Test
    fun playlistAndNonPlaylistApiMixed2() {
        val collector = IBitmovinPlayerCollector.create(appContext, TestConfig.createAnalyticsConfig())
        collector.setSourceMetadata(sourceMetadata1, source1)
        collector.setSourceMetadata(sourceMetadata2, source2)
        collector.setSourceMetadata(sourceMetadata3, source3)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(source2)

            // calling setCurrentSourceMetadata after source is loaded means
            // that it can be determined which source is currently active
            // and the sourceMetadata that is stored for this source is overwritten
            collector.setCurrentSourceMetadata(sourceMetadata2_2)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.load(source3)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            collector.detachPlayer()
        }

        // assert
        val impressionList = LogParser.extractImpressions()
        Assertions.assertThat(impressionList).hasSize(2)

        val firstImpression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(firstImpression)

        DataVerifier.verifyStaticData(firstImpression.eventDataList, sourceMetadata2_2, TestSources.DASH, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(firstImpression.eventDataList[0])

        val secondImpression = impressionList[1]
        DataVerifier.verifyHasNoErrorSamples(secondImpression)

        DataVerifier.verifyStaticData(secondImpression.eventDataList, sourceMetadata3, TestSources.PROGRESSIVE, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(secondImpression.eventDataList[0], isFirstImpression = false)
    }

    @Test
    fun playlistAndNonPlaylistApiMixed3() {
        val collector = IBitmovinPlayerCollector.create(appContext, TestConfig.createAnalyticsConfig())

        collector.setCurrentSourceMetadata(sourceMetadata2)
        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(source2)
            collector.setCurrentSourceMetadata(sourceMetadata2_2)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.load(source3)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            collector.detachPlayer()
        }

        Thread.sleep(300)

        // assert
        val impressionList = LogParser.extractImpressions()
        Assertions.assertThat(impressionList).hasSize(2)

        val firstImpression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(firstImpression)

        DataVerifier.verifyStaticData(firstImpression.eventDataList, sourceMetadata2_2, TestSources.DASH, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(firstImpression.eventDataList[0])

        val secondImpression = impressionList[1]
        DataVerifier.verifyHasNoErrorSamples(secondImpression)

        // sourceMetadata2_2 is set as generic sourceMetadata that is not bound to a specific source
        // since source3 is prog, the collector automatically detects the progUrl and uses sourceMetadata2_2
        DataVerifier.verifyStaticData(secondImpression.eventDataList, sourceMetadata2_2.copy(progUrl = sourceMetadata3.progUrl), TestSources.PROGRESSIVE, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(secondImpression.eventDataList[0], isFirstImpression = false)
    }

    @Test
    fun getCustomData_ShouldReturnMergedCustomData() {
        val defaultMetadata = DefaultMetadata(cdnProvider = "defaultCdnProvider", customData = TestConfig.createDummyCustomData("default"), customUserId = "userId")
        val collector = IBitmovinPlayerCollector.create(appContext, TestConfig.createAnalyticsConfig(), defaultMetadata)
        val sourceMetadata = SourceMetadata(cdnProvider = "sourceCdnProvider", customData = CustomData(customData1 = "customData1", customData10 = "customData10", customData30 = "data30"), m3u8Url = "dummyUrl", title = "dummyTitle", path = "dummyPath", videoId = "id")

        collector.setCurrentSourceMetadata(sourceMetadata)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(source1)
            defaultPlayer.play()
        }

        // wait a bit to make sure collector is attached to the player
        Thread.sleep(400)

        val mergedCustomData = collector.customData
        val expectedMergedCustomData = ApiV3Utils.mergeCustomData(sourceMetadata.customData, defaultMetadata.customData)
        Assertions.assertThat(mergedCustomData).isEqualTo(expectedMergedCustomData)

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            collector.detachPlayer()
        }

        Thread.sleep(300)

        // assert
        val impressionList = LogParser.extractImpressions()
        Assertions.assertThat(impressionList).hasSize(1)

        val firstImpression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(firstImpression)
        val expectedSourceMetadata = sourceMetadata.copy(customData = expectedMergedCustomData)
        DataVerifier.verifyStaticData(firstImpression.eventDataList, expectedSourceMetadata, TestSources.HLS_REDBULL, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(firstImpression.eventDataList[0])
    }

    @Test
    fun sourceMetadataIsOnlyUsedForAttachedSource() {
        val collector = IBitmovinPlayerCollector.create(appContext, TestConfig.createAnalyticsConfig())
        val defaultMetadata = DefaultMetadata(cdnProvider = "defaultCdnProvider", customUserId = "defaultCustomUserId", customData = TestConfig.createDummyCustomData("default"))

        collector.defaultMetadata = defaultMetadata
        collector.setSourceMetadata(sourceMetadata1, source1)

        // act
        mainScope.launch {
            collector.attachPlayer(defaultPlayer)
            defaultPlayer.load(source1)

            // calling setCurrentSourceMetadata after source is loaded means
            // that it can be determined which source is currently active
            // and the sourceMetadata that is stored for this source is overwritten
            collector.setCurrentSourceMetadata(sourceMetadata1)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            defaultPlayer.load(source2)
            defaultPlayer.play()
        }

        BitmovinPlaybackUtils.waitUntilPlayerPlayedToMs(defaultPlayer, 3000)

        mainScope.launch {
            defaultPlayer.pause()
            collector.detachPlayer()
        }

        Thread.sleep(300)

        // assert
        val impressionList = LogParser.extractImpressions()
        Assertions.assertThat(impressionList).hasSize(2)

        val firstImpression = impressionList.first()
        DataVerifier.verifyHasNoErrorSamples(firstImpression)

        DataVerifier.verifyStaticData(firstImpression.eventDataList, sourceMetadata1, TestSources.HLS_REDBULL, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(firstImpression.eventDataList[0])

        val secondImpression = impressionList[1]
        DataVerifier.verifyHasNoErrorSamples(secondImpression)

        // since there is no sourceMetadata set for source2, mpdUrl is determined from source info and customData/cdnProvider from defaultMetadata is used
        DataVerifier.verifyStaticData(secondImpression.eventDataList, SourceMetadata(mpdUrl = TestSources.DASH.mpdUrl, customData = defaultMetadata.customData, cdnProvider = defaultMetadata.cdnProvider), TestSources.DASH, BitmovinPlayerConstants.playerInfo)
        DataVerifier.verifyStartupSample(secondImpression.eventDataList[0], isFirstImpression = false)
    }
}
