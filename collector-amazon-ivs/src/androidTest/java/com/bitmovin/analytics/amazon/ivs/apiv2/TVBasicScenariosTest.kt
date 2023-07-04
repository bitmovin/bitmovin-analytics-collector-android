package com.bitmovin.analytics.amazon.ivs.apiv2

import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.IAmazonIvsPlayerCollector
import com.bitmovin.analytics.amazon.ivs.IvsPlayerConstants
import com.bitmovin.analytics.amazon.ivs.IvsTestUtils
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

// Instrumentation test for 4k TV testing, can only be run directly through intellij
// since TVs are not supported by gradle managed devices as of 2023-03-23
@RunWith(AndroidJUnit4::class)
class TVBasicScenariosTest {

    companion object {
        @BeforeClass @JvmStatic
        fun setup() {
            Looper.prepare()
        }
    }

    @Before
    fun markTestRun() {
        // logging to mark new test run for logparsing
        LogParser.startTracking()
    }

    @Test
    fun test_liveStream_basicPlayPauseScenario_Should_sendCorrectSamples() {
        // arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val liveSample = TestSources.IVS_LIVE_1

        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(liveSample.m3u8Url)
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(Uri.parse(liveSample.m3u8Url))
        IvsTestUtils.waitUntilPlayerIsReady(player)

        player.play()
        Thread.sleep(2000)
        player.pause()
        Thread.sleep(100)

        collector.detachPlayer()
        player.release()

        // assert
        val impressionsList = LogParser.extractImpressions()

        assertThat(impressionsList.size).isEqualTo(1)

        val impression = impressionsList.first()
        assertThat(impression.errorDetailList.size).isEqualTo(0)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, analyticsConfig, liveSample, IvsPlayerConstants.playerInfo, true)
        DataVerifier.verifyStartupSample(eventDataList[0])
    }
}
