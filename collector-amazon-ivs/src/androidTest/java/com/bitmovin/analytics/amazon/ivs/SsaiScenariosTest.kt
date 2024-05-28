package com.bitmovin.analytics.amazon.ivs

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.runner.RunWith

// System test for basic playing and error scenario using ivs player
// This tests assume a phone with api level >=30 for validations
// Tests can be run automatically with gradle managed device through running ./runSystemTests.sh in the root folder
@RunWith(AndroidJUnit4::class)
class SsaiScenariosTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var player: Player
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private val defaultSource = TestSources.IVS_LIVE_1
    private lateinit var mockedIngressUrl: String
    private val defaultSourceMetadata =
        SourceMetadata(
            customData = CustomData(customData1 = "custom-data-1"),
        )

    companion object {
        @BeforeClass @JvmStatic
        fun setupLooper() {
            Looper.prepare()
        }
    }

    @Before
    fun setup() {
        player = Player.Factory.create(appContext)
        player.isMuted = true
        player.setQuality(player.quality, false)
        player.isAutoQualityMode = false
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
    }

    @After
    fun teardown() {
        MockedIngress.stopServer()
    }

    // TODO fix flaky tests [https://bitmovin.atlassian.net/browse/AN-4144]
//    @Test
//    fun test_startAd_nextAd_stopAd_sets_right_values() {
//        // arrange
//        val collector = IAmazonIvsPlayerCollector.create(appContext, defaultAnalyticsConfig)
//        collector.sourceMetadata = defaultSourceMetadata
//
//        // act
//        collector.attachPlayer(player)
//        collector.ssai.startAdBreak(
//            SsaiAdPosition.PREROLL,
//            SsaiMetadata("test-ad-id-1", "test-ad-system-1"),
//            CustomData(customData1 = "ad-test-custom-data-1"),
//        )
//        player.load(Uri.parse(defaultSource.m3u8Url))
//        player.play()
//
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 1500)
//
//        collector.ssai.nextAd(SsaiMetadata("test-ad-id-2", "test-ad-system-2"), CustomData(customData2 = "ad-test-custom-data-2"))
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 3000)
//
//        collector.ssai.endAdBreak()
//        player.pause()
//
//        Thread.sleep(500)
//
//        collector.detachPlayer()
//        player.release()
//
//        Thread.sleep(500)
//
//        // assert
//        val impressionList = MockedIngress.extractImpressions()
//        assertThat(impressionList.size).isEqualTo(1)
//
//        val impression = impressionList.first()
//        DataVerifier.verifyHasNoErrorSamples(impression)
//
//        val eventDataList = impression.eventDataList
//        assertThat(eventDataList.size).isEqualTo(4)
//        val startupSample = eventDataList[0]
//        assertThat(startupSample.state).isEqualTo("startup")
//        assertThat(startupSample.startupTime).isGreaterThan(0)
//        assertThat(startupSample.ad).isEqualTo(2)
//        assertThat(startupSample.adIndex).isEqualTo(0)
//        assertThat(startupSample.customData1).isEqualTo("ad-test-custom-data-1")
//        assertThat(startupSample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
//        assertThat(startupSample.adSystem).isEqualTo("test-ad-system-1")
//        assertThat(startupSample.adId).isEqualTo("test-ad-id-1")
//
//        val playingAd1Sample = eventDataList[1]
//        assertThat(playingAd1Sample.state).isEqualTo("playing")
//        assertThat(playingAd1Sample.ad).isEqualTo(2)
//        assertThat(playingAd1Sample.adIndex).isEqualTo(1)
//        assertThat(playingAd1Sample.customData1).isEqualTo("ad-test-custom-data-1")
//        assertThat(playingAd1Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
//        assertThat(playingAd1Sample.adSystem).isEqualTo("test-ad-system-1")
//        assertThat(playingAd1Sample.adId).isEqualTo("test-ad-id-1")
//
//        val playingAd2Sample = eventDataList[2]
//        assertThat(playingAd2Sample.state).isEqualTo("playing")
//        assertThat(playingAd2Sample.ad).isEqualTo(2)
//        assertThat(playingAd2Sample.adIndex).isEqualTo(0)
//        assertThat(playingAd2Sample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(playingAd2Sample.customData2).isEqualTo("ad-test-custom-data-2")
//        assertThat(playingAd2Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
//        assertThat(playingAd2Sample.adSystem).isEqualTo("test-ad-system-2")
//        assertThat(playingAd2Sample.adId).isEqualTo("test-ad-id-2")
//
//        val regularPlayingSample = eventDataList[3]
//        assertThat(regularPlayingSample.state).isEqualTo("playing")
//        assertThat(regularPlayingSample.ad).isEqualTo(0)
//        assertThat(regularPlayingSample.adIndex).isNull()
//        assertThat(regularPlayingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(regularPlayingSample.customData2).isNull()
//        assertThat(regularPlayingSample.adPosition).isNull()
//        assertThat(regularPlayingSample.adSystem).isNull()
//        assertThat(regularPlayingSample.adId).isNull()
//    }
//
//    @Test
//    fun test_ignore_nextAd_call_if_startAd_has_not_been_called() {
//        // arrange
//        val collector = IAmazonIvsPlayerCollector.create(appContext, defaultAnalyticsConfig)
//        collector.sourceMetadata = defaultSourceMetadata
//
//        // act
//        collector.attachPlayer(player)
//        player.load(Uri.parse(defaultSource.m3u8Url))
//        player.play()
//
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 1500)
//
//        collector.ssai.nextAd(SsaiMetadata("test-ad-id-2", "test-ad-system-2"), CustomData(customData2 = "ad-test-custom-data-2"))
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 2000)
//
//        player.pause()
//        Thread.sleep(500)
//
//        collector.detachPlayer()
//
//        Thread.sleep(500)
//
//        // assert
//        val impressionList = MockedIngress.extractImpressions()
//        assertThat(impressionList.size).isEqualTo(1)
//
//        val impression = impressionList.first()
//        DataVerifier.verifyHasNoErrorSamples(impression)
//
//        val eventDataList = impression.eventDataList
//        assertThat(eventDataList.size).isEqualTo(2)
//        val startupSample = eventDataList[0]
//        assertThat(startupSample.state).isEqualTo("startup")
//        assertThat(startupSample.startupTime).isGreaterThan(0)
//        assertThat(startupSample.ad).isEqualTo(0)
//        assertThat(startupSample.adIndex).isNull()
//        assertThat(startupSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(startupSample.adPosition).isNull()
//        assertThat(startupSample.adSystem).isNull()
//        assertThat(startupSample.adId).isNull()
//
//        val playingSample = eventDataList[1]
//        assertThat(playingSample.state).isEqualTo("playing")
//        assertThat(playingSample.ad).isEqualTo(0)
//        assertThat(playingSample.adIndex).isNull()
//        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(playingSample.adPosition).isNull()
//        assertThat(playingSample.adSystem).isNull()
//        assertThat(playingSample.adId).isNull()
//    }
//
//    @Test
//    fun test_ignore_stopAd_call_if_startAd_has_not_been_called() {
//        // arrange
//        val collector = IAmazonIvsPlayerCollector.create(appContext, defaultAnalyticsConfig)
//        collector.sourceMetadata = defaultSourceMetadata
//
//        // act
//        collector.attachPlayer(player)
//        player.load(Uri.parse(defaultSource.m3u8Url))
//        player.play()
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 1500)
//
//        collector.ssai.endAdBreak()
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 3000)
//
//        player.pause()
//        Thread.sleep(500)
//
//        collector.detachPlayer()
//
//        Thread.sleep(500)
//
//        // assert
//        val impressionList = MockedIngress.extractImpressions()
//        assertThat(impressionList.size).isEqualTo(1)
//
//        val impression = impressionList.first()
//        DataVerifier.verifyHasNoErrorSamples(impression)
//
//        val eventDataList = impression.eventDataList
//        assertThat(eventDataList.size).isEqualTo(2)
//        val startupSample = eventDataList[0]
//        assertThat(startupSample.state).isEqualTo("startup")
//        assertThat(startupSample.startupTime).isGreaterThan(0)
//        assertThat(startupSample.ad).isEqualTo(0)
//        assertThat(startupSample.adIndex).isNull()
//        assertThat(startupSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(startupSample.adPosition).isNull()
//        assertThat(startupSample.adSystem).isNull()
//        assertThat(startupSample.adId).isNull()
//
//        val playingSample = eventDataList[1]
//        assertThat(playingSample.state).isEqualTo("playing")
//        assertThat(playingSample.ad).isEqualTo(0)
//        assertThat(playingSample.adIndex).isNull()
//        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(playingSample.adPosition).isNull()
//        assertThat(playingSample.adSystem).isNull()
//        assertThat(playingSample.adId).isNull()
//    }
//
//    @Test
//    fun test_increase_and_reset_adSequenceNumber() {
//        // arrange
//        val collector = IAmazonIvsPlayerCollector.create(appContext, defaultAnalyticsConfig)
//        collector.sourceMetadata = defaultSourceMetadata
//
//        // act
//        collector.attachPlayer(player)
//        player.load(Uri.parse(defaultSource.m3u8Url))
//        collector.ssai.startAdBreak(
//            SsaiAdPosition.PREROLL,
//            SsaiMetadata("test-ad-id-1", "test-ad-system-1"),
//            CustomData(customData1 = "ad-test-custom-data-1"),
//        )
//        player.play()
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 1500)
//
//        player.pause()
//        Thread.sleep(500)
//
//        player.play()
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 3000)
//
//        collector.ssai.nextAd(SsaiMetadata("test-ad-id-2", "test-ad-system-2"), CustomData(customData2 = "ad-test-custom-data-2"))
//        player.pause()
//        Thread.sleep(500)
//
//        player.play()
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 4000)
//
//        collector.ssai.endAdBreak()
//        player.pause()
//        Thread.sleep(500)
//
//        collector.detachPlayer()
//
//        Thread.sleep(500)
//
//        // assert
//        val impressionList = MockedIngress.extractImpressions()
//        assertThat(impressionList.size).isEqualTo(1)
//
//        val impression = impressionList.first()
//        DataVerifier.verifyHasNoErrorSamples(impression)
//
//        val eventDataList = impression.eventDataList.filter { it.state != "qualitychange" }
//        assertThat(eventDataList.size).isEqualTo(8)
//        val startupSample = eventDataList[0]
//        assertThat(startupSample.state).isEqualTo("startup")
//        assertThat(startupSample.startupTime).isGreaterThan(0)
//        assertThat(startupSample.ad).isEqualTo(2)
//        assertThat(startupSample.adIndex).isEqualTo(0)
//        assertThat(startupSample.customData1).isEqualTo("ad-test-custom-data-1")
//        assertThat(startupSample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
//        assertThat(startupSample.adSystem).isEqualTo("test-ad-system-1")
//        assertThat(startupSample.adId).isEqualTo("test-ad-id-1")
//
//        val playingAd1Sample = eventDataList[1]
//        assertThat(playingAd1Sample.state).isEqualTo("playing")
//        assertThat(playingAd1Sample.ad).isEqualTo(2)
//        assertThat(playingAd1Sample.adIndex).isEqualTo(1)
//        assertThat(playingAd1Sample.customData1).isEqualTo("ad-test-custom-data-1")
//        assertThat(playingAd1Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
//        assertThat(playingAd1Sample.adSystem).isEqualTo("test-ad-system-1")
//        assertThat(playingAd1Sample.adId).isEqualTo("test-ad-id-1")
//
//        val pausedAd1Sample = eventDataList[2]
//        assertThat(pausedAd1Sample.state).isEqualTo("pause")
//        assertThat(pausedAd1Sample.ad).isEqualTo(2)
//        assertThat(pausedAd1Sample.adIndex).isEqualTo(2)
//        assertThat(pausedAd1Sample.customData1).isEqualTo("ad-test-custom-data-1")
//        assertThat(pausedAd1Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
//        assertThat(pausedAd1Sample.adSystem).isEqualTo("test-ad-system-1")
//        assertThat(pausedAd1Sample.adId).isEqualTo("test-ad-id-1")
//
//        val playingAd1Sample2 = eventDataList[3]
//        assertThat(playingAd1Sample2.state).isEqualTo("playing")
//        assertThat(playingAd1Sample2.ad).isEqualTo(2)
//        assertThat(playingAd1Sample2.adIndex).isEqualTo(3)
//        assertThat(playingAd1Sample2.customData1).isEqualTo("ad-test-custom-data-1")
//        assertThat(playingAd1Sample2.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
//        assertThat(playingAd1Sample2.adSystem).isEqualTo("test-ad-system-1")
//        assertThat(playingAd1Sample2.adId).isEqualTo("test-ad-id-1")
//
//        val playingAd2Sample = eventDataList[4]
//        assertThat(playingAd2Sample.state).isEqualTo("playing")
//        assertThat(playingAd2Sample.ad).isEqualTo(2)
//        assertThat(playingAd2Sample.adIndex).isEqualTo(0)
//        assertThat(playingAd2Sample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(playingAd2Sample.customData2).isEqualTo("ad-test-custom-data-2")
//        assertThat(playingAd2Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
//        assertThat(playingAd2Sample.adSystem).isEqualTo("test-ad-system-2")
//        assertThat(playingAd2Sample.adId).isEqualTo("test-ad-id-2")
//
//        val pausedAd2Sample = eventDataList[5]
//        assertThat(pausedAd2Sample.state).isEqualTo("pause")
//        assertThat(pausedAd2Sample.ad).isEqualTo(2)
//        assertThat(pausedAd2Sample.adIndex).isEqualTo(1)
//        assertThat(pausedAd2Sample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(pausedAd2Sample.customData2).isEqualTo("ad-test-custom-data-2")
//        assertThat(pausedAd2Sample.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
//        assertThat(pausedAd2Sample.adSystem).isEqualTo("test-ad-system-2")
//        assertThat(pausedAd2Sample.adId).isEqualTo("test-ad-id-2")
//
//        val playingAd2Sample2 = eventDataList[6]
//        assertThat(playingAd2Sample2.state).isEqualTo("playing")
//        assertThat(playingAd2Sample2.ad).isEqualTo(2)
//        assertThat(playingAd2Sample2.adIndex).isEqualTo(2)
//        assertThat(playingAd2Sample2.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(playingAd2Sample2.customData2).isEqualTo("ad-test-custom-data-2")
//        assertThat(playingAd2Sample2.adPosition).isEqualTo(SsaiAdPosition.PREROLL.toString())
//        assertThat(playingAd2Sample2.adSystem).isEqualTo("test-ad-system-2")
//        assertThat(playingAd2Sample2.adId).isEqualTo("test-ad-id-2")
//
//        val regularPlayingSample = eventDataList[7]
//        assertThat(regularPlayingSample.state).isEqualTo("playing")
//        assertThat(regularPlayingSample.ad).isEqualTo(0)
//        assertThat(regularPlayingSample.adIndex).isNull()
//        assertThat(regularPlayingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(regularPlayingSample.customData2).isNull()
//        assertThat(regularPlayingSample.adPosition).isNull()
//        assertThat(regularPlayingSample.adSystem).isNull()
//        assertThat(regularPlayingSample.adId).isNull()
//    }
//
//    @Test
//    fun test_ignores_startAd_nextAd_and_endAd_when_player_is_paused() {
//        // arrange
//        val collector = IAmazonIvsPlayerCollector.create(appContext, defaultAnalyticsConfig)
//        collector.sourceMetadata = defaultSourceMetadata
//
//        // act
//        collector.attachPlayer(player)
//        player.load(Uri.parse(defaultSource.m3u8Url))
//        player.play()
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 1500)
//
//        player.pause()
//        Thread.sleep(500)
//
//        collector.ssai.startAdBreak(
//            SsaiAdPosition.PREROLL,
//            SsaiMetadata("test-ad-id-1", "test-ad-system-1"),
//            CustomData(customData1 = "ad-test-custom-data-1"),
//        )
//        collector.ssai.nextAd(SsaiMetadata("test-ad-id-2", "test-ad-system-2"), CustomData(customData2 = "ad-test-custom-data-2"))
//        collector.ssai.endAdBreak()
//        player.play()
//
//        IvsTestUtils.waitUntilPlayerPlayedToMs(player, 2000)
//
//        player.pause()
//        Thread.sleep(500)
//
//        collector.detachPlayer()
//
//        Thread.sleep(500)
//
//        // assert
//        val impressionList = MockedIngress.extractImpressions()
//        assertThat(impressionList.size).isEqualTo(1)
//
//        val impression = impressionList.first()
//        DataVerifier.verifyHasNoErrorSamples(impression)
//
//        val eventDataList = impression.eventDataList
//        assertThat(eventDataList.size).isEqualTo(4)
//        val startupSample = eventDataList[0]
//        assertThat(startupSample.state).isEqualTo("startup")
//        assertThat(startupSample.startupTime).isGreaterThan(0)
//        assertThat(startupSample.sequenceNumber).isEqualTo(0)
//        assertThat(startupSample.ad).isEqualTo(0)
//        assertThat(startupSample.adIndex).isNull()
//        assertThat(startupSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(startupSample.adPosition).isNull()
//        assertThat(startupSample.adSystem).isNull()
//        assertThat(startupSample.adId).isNull()
//
//        val playingSample = eventDataList[1]
//        assertThat(playingSample.state).isEqualTo("playing")
//        assertThat(playingSample.startupTime).isEqualTo(0)
//        assertThat(playingSample.sequenceNumber).isEqualTo(1)
//        assertThat(playingSample.ad).isEqualTo(0)
//        assertThat(playingSample.adIndex).isNull()
//        assertThat(playingSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(playingSample.adPosition).isNull()
//        assertThat(playingSample.adSystem).isNull()
//        assertThat(playingSample.adId).isNull()
//
//        val pauseSample = eventDataList[2]
//        assertThat(pauseSample.state).isEqualTo("pause")
//        assertThat(pauseSample.startupTime).isEqualTo(0)
//        assertThat(pauseSample.sequenceNumber).isEqualTo(2)
//        assertThat(pauseSample.ad).isEqualTo(0)
//        assertThat(pauseSample.adIndex).isNull()
//        assertThat(pauseSample.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(pauseSample.adPosition).isNull()
//        assertThat(pauseSample.adSystem).isNull()
//        assertThat(pauseSample.adId).isNull()
//
//        val playingSample2 = eventDataList[3]
//        assertThat(playingSample2.state).isEqualTo("playing")
//        assertThat(playingSample2.startupTime).isEqualTo(0)
//        assertThat(playingSample2.sequenceNumber).isEqualTo(3)
//        assertThat(playingSample2.ad).isEqualTo(0)
//        assertThat(playingSample2.adIndex).isNull()
//        assertThat(playingSample2.customData1).isEqualTo(defaultSourceMetadata.customData.customData1)
//        assertThat(playingSample2.adPosition).isNull()
//        assertThat(playingSample2.adSystem).isNull()
//        assertThat(playingSample2.adId).isNull()
//    }
}
