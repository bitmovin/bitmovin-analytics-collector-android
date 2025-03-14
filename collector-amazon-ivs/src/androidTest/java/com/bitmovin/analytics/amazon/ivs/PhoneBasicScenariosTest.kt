package com.bitmovin.analytics.amazon.ivs
import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.api.IAmazonIvsPlayerCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.EventDataUtils
import com.bitmovin.analytics.systemtest.utils.MetadataUtils
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.PlayerSettings
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import com.bitmovin.analytics.systemtest.utils.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// System test for basic playing and error scenario using ivs player
// This tests assume a phone with api level >=30 for validations
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh in the root folder
@RunWith(AndroidJUnit4::class)
class PhoneBasicScenariosTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var player: Player
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

    @get:Rule
    val metadataGenerator = MetadataUtils.MetadataGenerator()

    companion object {
        @BeforeClass @JvmStatic
        fun setupLooper() {
            if (Looper.myLooper() == null) {
                Looper.prepare()
            }
        }
    }

    @Before
    fun setup() {
        player = Player.Factory.create(appContext)
        player.isMuted = true
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
    }

    @After
    fun teardown() {
        MockedIngress.stopServer()
    }

    @Test
    fun test_live_playPause() =
        runBlockingTest {
            // arrange
            val liveStreamSample = TestSources.IVS_LIVE_1
            val collector = IAmazonIvsPlayerCollector.create(appContext, defaultAnalyticsConfig)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    isLive = true,
                    videoId = "videoId1",
                    customData = TestConfig.createDummyCustomData("ivsLive1_"),
                )
            collector.sourceMetadata = sourceMetadata
            collector.attachPlayer(player)

            // act
            player.load(Uri.parse(liveStreamSample.m3u8Url))
            player.play()
            IvsTestUtils.waitUntilPlayerPlayedToMs(player, 4000)

            player.pause()
            val firstPauseMs = 500L
            Thread.sleep(firstPauseMs)
            player.play()
            IvsTestUtils.waitUntilPlayerPlayedToMs(player, 9000)
            player.pause()
            Thread.sleep(100)

            collector.detachPlayer()
            player.release()

            // assert
            val impressionSamples = MockedIngress.waitForRequestsAndExtractImpressions()

            // only one impression is generated and no errors are sent
            assertThat(impressionSamples.size).isEqualTo(1)
            assertThat(impressionSamples.first().errorDetailList.size).isEqualTo(0)

            val eventDataList = impressionSamples.first().eventDataList

            DataVerifier.verifyStaticData(eventDataList, sourceMetadata, liveStreamSample, IvsPlayerConstants.playerInfo)
            DataVerifier.verifyInvariants(eventDataList)
            DataVerifier.verifyM3u8SourceUrl(eventDataList, liveStreamSample.m3u8Url!!)

            val filteredList = EventDataUtils.filterNonDeterministicEvents(eventDataList)

            // there need to be at least 4 events
            // startup, playing, pause, playing
            assertThat(filteredList).hasSizeGreaterThanOrEqualTo(4)

            DataVerifier.verifyStartupSample(eventDataList[0])
            assertThat(filteredList[1].state).isEqualTo("playing")

            // verify that there is exactly one pause sample
            val pauseSamples = filteredList.filter { x -> x.state?.lowercase() == "pause" }
            assertThat(pauseSamples.size).isEqualTo(1)
            assertThat(pauseSamples[0].paused).isGreaterThan(firstPauseMs - 100) // reducing minimal expected pause time to make test stable
        }

    @Test
    fun test_live_2ImpressionsScenario() =
        runBlockingTest {
            // arrange
            player.isMuted = false
            val liveStreamSample1 = TestSources.IVS_LIVE_1
            val collector = IAmazonIvsPlayerCollector.create(appContext, defaultAnalyticsConfig)

            val sourceMetadata1 =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("Src1"),
                    isLive = true,
                    videoId = "videoId1",
                    customData = TestConfig.createDummyCustomData("ivsLive1_"),
                )

            collector.sourceMetadata = sourceMetadata1
            collector.attachPlayer(player)
            // act
            player.load(Uri.parse(liveStreamSample1.m3u8Url))
            player.play()
            IvsTestUtils.waitUntilPlayerIsPlaying(player)

            val firstPlayMs = 3000L
            Thread.sleep(firstPlayMs)

            player.pause()
            Thread.sleep(100)

            collector.detachPlayer()

            val liveStreamSample2 = TestSources.IVS_LIVE_2
            val sourceMetadata2 =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle("Src2"),
                    isLive = true,
                    videoId = "videoId2",
                    customData = TestConfig.createDummyCustomData("ivsLive2_"),
                )

            collector.sourceMetadata = sourceMetadata2
            player.isMuted = true
            collector.attachPlayer(player)

            player.load(Uri.parse(liveStreamSample2.m3u8Url))
            player.play()
            IvsTestUtils.waitUntilPlayerIsPlaying(player)
            val secondPlayMs = 5000L
            Thread.sleep(secondPlayMs)

            player.pause()
            Thread.sleep(100)

            collector.detachPlayer()
            player.release()

            // assert
            val impressionList = MockedIngress.waitForRequestsAndExtractImpressions()

            assertThat(impressionList).hasSize(2)
            val firstImpressionSamples = impressionList[0].eventDataList
            val secondImpressionSamples = impressionList[1].eventDataList

            // verify that two session have different impression_id
            assertThat(firstImpressionSamples.first().impressionId).isNotEqualTo(secondImpressionSamples.first().impressionId)

            DataVerifier.verifyStaticData(firstImpressionSamples, sourceMetadata1, liveStreamSample1, IvsPlayerConstants.playerInfo)
            DataVerifier.verifyStaticData(secondImpressionSamples, sourceMetadata2, liveStreamSample2, IvsPlayerConstants.playerInfo)
            DataVerifier.verifyPlayerSetting(firstImpressionSamples, PlayerSettings(false))
            DataVerifier.verifyPlayerSetting(secondImpressionSamples, PlayerSettings(true))
            DataVerifier.verifyInvariants(firstImpressionSamples)
            DataVerifier.verifyInvariants(secondImpressionSamples)
            DataVerifier.verifyM3u8SourceUrl(firstImpressionSamples, liveStreamSample1.m3u8Url!!)
            DataVerifier.verifyM3u8SourceUrl(secondImpressionSamples, liveStreamSample2.m3u8Url!!)

            val filteredFirst = EventDataUtils.filterNonDeterministicEvents(firstImpressionSamples)
            val filteredSecond = EventDataUtils.filterNonDeterministicEvents(secondImpressionSamples)

            // there need to be at least 2 events per session
            // startup, playing
            assertThat(filteredFirst).hasSizeGreaterThanOrEqualTo(2)
            assertThat(filteredSecond).hasSizeGreaterThanOrEqualTo(2)

            DataVerifier.verifyStartupSample(filteredFirst.first())
            DataVerifier.verifyStartupSample(filteredSecond.first(), false)

            assertThat(filteredFirst[1].state).isEqualTo("playing")
            assertThat(filteredSecond[1].state).isEqualTo("playing")

            // verify durations of playing samples state are within a reasonable range
            DataVerifier.verifyPlayTimeIsCorrect(filteredFirst, firstPlayMs)
            DataVerifier.verifyPlayTimeIsCorrect(filteredSecond, secondPlayMs)
        }

    @Test
    fun test_vod_playSeekWithAutoplay() =
        runBlockingTest {
            val vodStreamSample = TestSources.IVS_VOD_1
            val collector = IAmazonIvsPlayerCollector.Factory.create(appContext, defaultAnalyticsConfig)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    isLive = false,
                    videoId = "videoId",
                    customData = TestConfig.createDummyCustomData("ivsVod_"),
                )
            collector.sourceMetadata = sourceMetadata
            collector.attachPlayer(player)

            // act
            player.load(Uri.parse(vodStreamSample.m3u8Url))
            player.play()

            val playedBeforeSeekMs = 2000L
            IvsTestUtils.waitUntilPlayerPlayedToMs(player, playedBeforeSeekMs)

            player.seekTo(1000)
            IvsTestUtils.waitUntilPlayerPlayedToMs(player, 3000)
            player.pause()
            Thread.sleep(500)

            collector.detachPlayer()
            player.release()

            // assert
            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionsList).hasSize(1)

            val impression = impressionsList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyStaticData(eventDataList, sourceMetadata, vodStreamSample, IvsPlayerConstants.playerInfo)
            DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
            DataVerifier.verifyM3u8SourceUrl(eventDataList, vodStreamSample.m3u8Url!!)

            val filteredList = EventDataUtils.filterNonDeterministicEvents(eventDataList)

            // there need to be at least 3 events
            // startup, playing, seeking
            assertThat(filteredList).hasSizeGreaterThanOrEqualTo(3)

            DataVerifier.verifyStartupSample(filteredList[0])
            DataVerifier.verifyThereWasExactlyOneSeekingSample(filteredList)
        }

    @Test
    fun test_vod_play() =
        runBlockingTest {
            // arrange
            val vodStreamSample = TestSources.IVS_VOD_1
            val collector = IAmazonIvsPlayerCollector.Factory.create(appContext, defaultAnalyticsConfig)
            collector.attachPlayer(player)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    isLive = false,
                    videoId = "videoId",
                    customData = TestConfig.createDummyCustomData("ivsVod_"),
                )
            collector.sourceMetadata = sourceMetadata

            // act
            player.load(Uri.parse(vodStreamSample.m3u8Url))
            IvsTestUtils.waitUntilPlayerIsReady(player)

            player.play()

            val playedBeforePause = 3000L
            IvsTestUtils.waitUntilPlayerPlayedToMs(player, playedBeforePause)

            player.pause()
            Thread.sleep(100)

            collector.detachPlayer()
            player.release()

            // assert
            val impressionsList = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressionsList).hasSize(1)

            val impression = impressionsList.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            DataVerifier.verifyStaticData(eventDataList, sourceMetadata, vodStreamSample, IvsPlayerConstants.playerInfo)
            DataVerifier.verifyM3u8SourceUrl(eventDataList, vodStreamSample.m3u8Url!!)
            DataVerifier.verifyVideoStartEndTimesOnContinuousPlayback(eventDataList)
            DataVerifier.verifyPlayerSetting(eventDataList, PlayerSettings(true))
            DataVerifier.verifyInvariants(eventDataList)

            val filteredList = EventDataUtils.filterNonDeterministicEvents(eventDataList)

            // there need to be at least 2 events
            // startup, playing
            assertThat(filteredList).hasSizeGreaterThanOrEqualTo(2)
            DataVerifier.verifyStartupSample(filteredList[0])

            DataVerifier.verifyPlayTimeIsCorrect(filteredList, playedBeforePause)
        }

    @Test
    fun test_nonExistingStream_Should_sendErrorSample() =
        runBlockingTest {
            // arrange
            val nonExistingStreamSample = Samples.NONE_EXISTING_STREAM
            val collector = IAmazonIvsPlayerCollector.create(appContext, defaultAnalyticsConfig)
            val nonExistingStreamSourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    isLive = false,
                    customData = TestConfig.createDummyCustomData("noneExitingStreamData"),
                )
            collector.sourceMetadata = nonExistingStreamSourceMetadata
            collector.attachPlayer(player)

            // act
            player.load(nonExistingStreamSample.uri)

            Thread.sleep(4000) // we need to wait a bit until player goes into error state

            collector.detachPlayer()
            player.release()

            // assert
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            val impression = impressions.first()

            assertThat(impression.eventDataList.size).isEqualTo(1)
            val eventData = impression.eventDataList.first()
            val impressionId = eventData.impressionId
            assertThat(eventData.errorMessage).isEqualTo("ERROR_NOT_AVAILABLE")
            assertThat(eventData.errorCode).isEqualTo(11)

            DataVerifier.verifyStartupSampleOnError(eventData, IvsPlayerConstants.playerInfo)
            DataVerifier.verifySourceMetadata(eventData, nonExistingStreamSourceMetadata)
            DataVerifier.verifyM3u8SourceUrl(impression.eventDataList, nonExistingStreamSample.uri.toString())

            assertThat(impression.errorDetailList).hasSize(1)
            val errorDetail = impression.errorDetailList.first()
            DataVerifier.verifyStaticErrorDetails(errorDetail, impressionId, defaultAnalyticsConfig.licenseKey)
            assertThat(errorDetail.data.exceptionStacktrace).hasSizeGreaterThan(0)
            assertThat(errorDetail.data.exceptionMessage).isEqualTo("MasterPlaylist : ERROR_NOT_AVAILABLE : 404 : Failed to load playlist")
        }

    @Test
    fun test_wrongAnalyticsLicense_ShouldNotInterfereWithPlayer() =
        runBlockingTest {
            // arrange
            val sample = TestSources.IVS_VOD_1
            val analyticsConfig = TestConfig.createAnalyticsConfig("nonExistingKey", backendUrl = mockedIngressUrl)
            val collector = IAmazonIvsPlayerCollector.Factory.create(appContext, analyticsConfig)
            collector.attachPlayer(player)
            player.load(Uri.parse(sample.m3u8Url))

            // act
            player.play()
            IvsTestUtils.waitUntilPlayerPlayedToMs(player, 2000)

            player.pause()
            collector.detachPlayer()
            player.release()

            // wait a bit, to make sure potential samples would have been sent to ingress
            Thread.sleep(300)

            // assert that no samples are sent
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions).hasSize(0)
        }

    @Test
    fun test_sendCustomDataEvent() =
        runBlockingTest {
            // arrange
            val vodStreamSample = TestSources.IVS_VOD_1
            val collector = IAmazonIvsPlayerCollector.Factory.create(appContext, defaultAnalyticsConfig)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    isLive = false,
                    videoId = "videoId",
                    customData = TestConfig.createDummyCustomData("ivsVod_"),
                )
            collector.sourceMetadata = sourceMetadata
            val customData1 = TestConfig.createDummyCustomData("customData1")
            val customData2 = TestConfig.createDummyCustomData("customData2")
            val customData3 = TestConfig.createDummyCustomData("customData3")
            val customData4 = TestConfig.createDummyCustomData("customData4")
            val customData5 = TestConfig.createDummyCustomData("customData5")

            // act
            collector.sendCustomDataEvent(customData1) // since we are not attached this shouldn't be sent
            collector.attachPlayer(player)

            player.load(Uri.parse(vodStreamSample.m3u8Url))
            collector.sendCustomDataEvent(customData2)

            player.play()

            IvsTestUtils.waitUntilPlayerPlayedToMs(player, 2001)
            collector.sendCustomDataEvent(customData3)

            player.pause()

            collector.sendCustomDataEvent(customData4)

            // wait a bit before detaching to make sure customData4 is sent out (to stabilize test)
            Thread.sleep(200)

            collector.detachPlayer()
            player.release()
            collector.sendCustomDataEvent(customData5) // this event should not be sent since collector is detached

            Thread.sleep(300)

            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions.size).isEqualTo(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            val customDataEvents = eventDataList.filter { it.state == "customdatachange" }

            assertThat(customDataEvents).hasSize(3)
            DataVerifier.verifySourceMetadata(customDataEvents[0], sourceMetadata.copy(customData = customData2))
            assertThat(customDataEvents[0].videoTimeStart).isEqualTo(0)
            assertThat(customDataEvents[0].videoTimeEnd).isEqualTo(0)

            DataVerifier.verifySourceMetadata(customDataEvents[1], sourceMetadata.copy(customData = customData3))
            assertThat(customDataEvents[1].videoTimeStart).isNotEqualTo(0)
            assertThat(customDataEvents[1].videoTimeEnd).isNotEqualTo(0)

            DataVerifier.verifySourceMetadata(customDataEvents[2], sourceMetadata.copy(customData = customData4))
            assertThat(customDataEvents[2].videoTimeStart).isGreaterThan(2000)
            assertThat(customDataEvents[2].videoTimeEnd).isGreaterThan(2000)
        }

    @Test
    fun test_changeCustomDataWhilePlaying() =
        runBlockingTest {
            // arrange
            val vodStreamSample = TestSources.IVS_VOD_1
            val defaultCustomData = CustomData(customData1 = "v1.2.3", customData2 = "videoID123")
            val defaultMetadata =
                DefaultMetadata(cdnProvider = "testCdnPovider", customUserId = "testCustomUserId", customData = defaultCustomData)
            val collector = IAmazonIvsPlayerCollector.Factory.create(appContext, defaultAnalyticsConfig, defaultMetadata)
            val sourceMetadata =
                SourceMetadata(
                    title = metadataGenerator.getTestTitle(),
                    isLive = false,
                    videoId = "videoId",
                    customData = CustomData(customData3 = "beforeSetCustomData"),
                )
            collector.sourceMetadata = sourceMetadata

            // act
            collector.attachPlayer(player)
            player.load(Uri.parse(vodStreamSample.m3u8Url))
            player.play()

            IvsTestUtils.waitUntilPlayerPlayedToMs(player, 2000)
            collector.customData = CustomData(customData3 = "afterSetCustomData")
            IvsTestUtils.waitUntilPlayerPlayedToMs(player, 10000)
            player.pause()

            collector.detachPlayer()
            player.release()

            // assert
            val impressions = MockedIngress.waitForRequestsAndExtractImpressions()
            assertThat(impressions.size).isEqualTo(1)

            val impression = impressions.first()
            DataVerifier.verifyHasNoErrorSamples(impression)

            val eventDataList = impression.eventDataList
            val beforeCustomDataChange = eventDataList.filter { it.customData3 == "beforeSetCustomData" }
            val afterCustomDataChange = eventDataList.filter { it.customData3 == "afterSetCustomData" }

            assertThat(beforeCustomDataChange).hasSizeGreaterThanOrEqualTo(2)
            assertThat(afterCustomDataChange).hasSizeGreaterThanOrEqualTo(1)

            // make sure that setCustomData closed all sample
            assertThat(beforeCustomDataChange.last().videoTimeEnd).isBetween(2000, 2150)
            assertThat(afterCustomDataChange[0].videoTimeStart).isBetween(2000, 2150)

            eventDataList.forEach {
                assertThat(it.customData1).isEqualTo(defaultCustomData.customData1)
                assertThat(it.customData2).isEqualTo(defaultCustomData.customData2)
                assertThat(it.customUserId).isEqualTo(defaultMetadata.customUserId)
                assertThat(it.cdnProvider).isEqualTo(defaultMetadata.cdnProvider)
            }
        }
}
