package com.bitmovin.analytics.amazon.ivs

import android.net.Uri
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.api.IAmazonIvsPlayerCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.systemtest.utils.DataVerifier
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.TestConfig
import com.bitmovin.analytics.systemtest.utils.TestSources
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

// Instrumentation test for 4k TV testing, can only be run directly through intellij
// since TVs are not supported by gradle managed devices as of 2023-03-23
@RunWith(AndroidJUnit4::class)
class TVBasicScenariosTest {
    private lateinit var defaultAnalyticsConfig: AnalyticsConfig
    private lateinit var mockedIngressUrl: String

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
        mockedIngressUrl = MockedIngress.startServer()
        defaultAnalyticsConfig = TestConfig.createAnalyticsConfig(backendUrl = mockedIngressUrl)
    }

    @After
    fun teardown() {
        MockedIngress.stopServer()
    }

    @Test
    fun test_liveStream_basicPlayPauseScenario_Should_sendCorrectSamples() {
        // arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val liveSample = TestSources.IVS_LIVE_1

        val sourceMetadata = SourceMetadata(title = "tvTest", customData = TestConfig.createDummyCustomData("tvTest"))
        val collector = IAmazonIvsPlayerCollector.create(appContext, defaultAnalyticsConfig)
        collector.attachPlayer(player)
        collector.sourceMetadata = sourceMetadata

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
        val impressionsList = MockedIngress.extractImpressions()

        assertThat(impressionsList.size).isEqualTo(1)

        val impression = impressionsList.first()
        assertThat(impression.errorDetailList.size).isEqualTo(0)

        val eventDataList = impression.eventDataList
        DataVerifier.verifyStaticData(eventDataList, sourceMetadata, liveSample, IvsPlayerConstants.playerInfo, true)
        DataVerifier.verifyStartupSample(eventDataList[0])
    }
}
