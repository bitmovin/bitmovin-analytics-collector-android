package com.bitmovin.analytics.amazon.ivs

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.LogParser
import com.bitmovin.analytics.systemtest.utils.StreamData
import com.bitmovin.analytics.systemtest.utils.TestConfig
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

    @Test
    fun test_liveStream_basicPlayPauseScenario_Should_sendCorrectSamples() {
        // arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val testSample = Samples.ivsLiveStream1Source

        val analyticsConfig = TestConfig.createBitmovinAnalyticsConfig(testSample.uri.toString())
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(testSample.uri)

        player.play()
        Thread.sleep(4000)
        player.pause()
        Thread.sleep(1200)
        player.play()
        Thread.sleep(5000)
        player.pause()
        Thread.sleep(1000)

        collector.detachPlayer()
        player.release()

        // assert
        val eventDataList = LogParser.extractAnalyticsSamplesFromLogs()
        val expectedStreamData = StreamData(
            "avc1.4D401F",
            "mp4a.40.2",
            testSample.uri.toString(),
            "hls",
            true,
            -1,
        )

        DataVerifier.verifyStaticData(eventDataList, analyticsConfig, expectedStreamData, IvsPlayerConstants.playerInfo, true)
        DataVerifier.verifyStartupSample(eventDataList[0])
    }
}
