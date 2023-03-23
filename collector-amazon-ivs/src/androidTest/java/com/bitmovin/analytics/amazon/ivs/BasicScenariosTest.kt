package com.bitmovin.analytics.amazon.ivs

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.example.shared.Samples
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import com.bitmovin.analytics.utils.DataSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader

@RunWith(AndroidJUnit4::class)
class BasicScenariosTest {

    companion object {
        @BeforeClass @JvmStatic
        fun setup() {
            Looper.prepare()
        }
    }

    @Test
    fun test_basicPlayPauseScenario_Should_sendCorrectSamples() {
        // arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val analyticsConfig = createBitmovinAnalyticsConfig()
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)

        // act
        player.load(Samples.ivsLiveStream1Source.uri)

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
        val eventDataList = extractAnalyticsSamplesFromLogs()

        // make sure that these properties are static over the whole session
        val generatedUserId = eventDataList[0].userId
        val impression_id = eventDataList[0].impressionId

        // verify data that should be present in all samples
        for (eventData in eventDataList) {
            verifyDeviceInfo(eventData)
            verifyAnalyticsConfig(eventData, analyticsConfig)
            verifyPlayerAndCollectorInfo(eventData)
            verifyStreamData(eventData)
            verifyUserAgent(eventData)

            assertThat(eventData.impressionId).isEqualTo(impression_id)
            assertThat(eventData.userId).isEqualTo(generatedUserId)
            assertThat(eventData.videoStartFailed).isFalse

            // live stream has duration -1
            assertThat(eventData.videoDuration).isEqualTo(-1)
        }

        // We filter for onqualitychange and buffering events
        // since they are non deterministic and would probably make the test flaky
        eventDataList.removeAll { x -> x.state?.lowercase() == "onqualitychange" }
        eventDataList.removeAll { x -> x.state?.lowercase() == "buffering" }

        // there need to be at least 4 events
        // startup, playing, pause, playing
        assertThat(eventDataList.size).isGreaterThanOrEqualTo(4)

        verifyStartupSample(eventDataList[0])
        assertThat(eventDataList[1].state).isEqualTo("playing")

        // verify that there is exactly one pause sample
        val pauseSamples = eventDataList.filter { x -> x.state?.lowercase() == "pause" }
        assertThat(pauseSamples.size).isEqualTo(1)
        assertThat(pauseSamples[0].paused).isGreaterThan(1000)
    }

    @Test
    fun test_errorScenario_Should_sendErrorSample() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val player = Player.Factory.create(appContext)
        player.isMuted = true

        val analyticsConfig = createBitmovinAnalyticsConfig()
        val collector = IAmazonIvsPlayerCollector.create(analyticsConfig, appContext)
        collector.attachPlayer(player)
        player.load(Samples.nonExistingStream.uri)

        player.play()
        Thread.sleep(5000)

        val eventDataList = extractHttpClientJsonLogLines()

        // remove license call
        eventDataList.removeFirst()

        val eventData = DataSerializer.deserialize(
            eventDataList[0],
            EventData::class.java,
        )

        val impressionId = eventData?.impressionId

        assertThat(eventData?.errorMessage).isEqualTo("ERROR_NOT_AVAILABLE")
        assertThat(eventData?.errorCode).isEqualTo(11)
        assertThat(eventData?.videoStartFailed).isTrue

        val errorDetail = DataSerializer.deserialize(
            eventDataList[1],
            ErrorDetail::class.java,
        )

        assertThat(errorDetail?.data?.exceptionStacktrace?.size).isGreaterThan(0)
        assertThat(errorDetail?.data?.exceptionMessage).isEqualTo("MasterPlaylist : ERROR_NOT_AVAILABLE : 404 : Failed to load playlist")
        assertThat(errorDetail?.impressionId).isEqualTo(impressionId)
        assertThat(errorDetail?.platform).isEqualTo("android")
        assertThat(errorDetail?.licenseKey).isEqualTo(analyticsConfig.key)
    }

    private fun extractAnalyticsSamplesFromLogs(): MutableList<EventData> {
        val analyticsSamplesStrings = extractHttpClientJsonLogLines()

        // remove the first network requests since it is the license call
        val licenseCallStringJson = analyticsSamplesStrings.removeFirst()

        val eventDataList = mutableListOf<EventData>()

        for (sample in analyticsSamplesStrings) {
            val eventData = DataSerializer.deserialize(
                sample,
                EventData::class.java,
            )

            if (eventData != null) {
                eventDataList.add(eventData)
            }
        }

        return eventDataList
    }

    private fun extractHttpClientJsonLogLines(): MutableList<String> {
        val logLines = mutableListOf<String>()
        val process = Runtime.getRuntime().exec("logcat -d")
        val bufferedReader = BufferedReader(
            InputStreamReader(process.inputStream),
        )

        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            logLines.add(line!!)
        }

        // find starting of logs of most recent test run (this is a bit of a hack because I couldn't get
        // clearing of logcat after a test run working)
        val testRunLogStartedIdx = logLines.indexOfLast { x -> x.contains("TestRunner: started:") }
        // filter for log lines that contain the network requests
        val testRunLines = logLines.subList(testRunLogStartedIdx, logLines.size)

        val jsonRegex = """\{.*\}$""".toRegex()
        val analyticsSamplesStrings = testRunLines.filter { x -> x.contains("HttpClient: {") }
            .map { x -> jsonRegex.find(x)?.value }
            .toMutableList()
            .filterNotNull()

        return analyticsSamplesStrings.toMutableList()
    }

    private fun createBitmovinAnalyticsConfig(): BitmovinAnalyticsConfig {
        /** Account: 'bitmovin-analytics', Analytics License: 'Local Development License Key" */
        val bitmovinAnalyticsConfig =
            BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")

        bitmovinAnalyticsConfig.title = "Android Amazon IVS player video"
        bitmovinAnalyticsConfig.videoId = "IVS video id"
        bitmovinAnalyticsConfig.customUserId = "customBitmovinUserId1"
        bitmovinAnalyticsConfig.experimentName = "experiment-1"
        bitmovinAnalyticsConfig.customData1 = "customData1"
        bitmovinAnalyticsConfig.customData2 = "customData2"
        bitmovinAnalyticsConfig.customData3 = "customData3"
        bitmovinAnalyticsConfig.customData4 = "customData4"
        bitmovinAnalyticsConfig.customData5 = "customData5"
        bitmovinAnalyticsConfig.customData6 = "customData6"
        bitmovinAnalyticsConfig.customData7 = "customData7"
        bitmovinAnalyticsConfig.path = "/customPath/new/"
        bitmovinAnalyticsConfig.m3u8Url = Samples.ivsLiveStream1Source.uri.toString()
        bitmovinAnalyticsConfig.cdnProvider = "testCdnProvider"
        return bitmovinAnalyticsConfig
    }

    private fun verifyStartupSample(eventData: EventData) {
        assertThat(eventData.startupTime).isGreaterThan(0)
        assertThat(eventData.state).isEqualTo("startup")
        assertThat(eventData.supportedVideoCodecs).isNotNull
        assertThat(eventData.playerStartupTime).isEqualTo(1)
        assertThat(eventData.videoStartupTime).isGreaterThan(0)
    }

    private fun verifyDeviceInfo(eventData: EventData) {
        assertThat(eventData.deviceInformation.model).isNotEmpty
        assertThat(eventData.deviceInformation.isTV).isFalse
        assertThat(eventData.deviceInformation.manufacturer).isNotEmpty
        assertThat(eventData.screenWidth).isGreaterThan(0)
        assertThat(eventData.screenHeight).isGreaterThan(0)
        assertThat(eventData.platform).isEqualTo("android")
    }

    private fun verifyPlayerAndCollectorInfo(eventData: EventData) {
        assertThat(eventData.player).isEqualTo("amazonivs")
        assertThat(eventData.playerTech).isEqualTo("Android:AmazonIVS")

        // version is dynamic, this we only check the first part of the version string
        assertThat(eventData.version).startsWith("amazonivs-1.")

        assertThat(eventData.analyticsVersion).isEqualTo("0.0.0-local")
    }

    private fun verifyUserAgent(eventData: EventData) {
        assertThat(eventData.userAgent).isNotEmpty
        assertThat(eventData.userAgent).contains("Android 11")
    }

    private fun verifyStreamData(eventData: EventData) {
        assertThat(eventData.audioCodec).isEqualTo("mp4a.40.2")
        assertThat(eventData.videoCodec).isEqualTo("avc1.4D401F")
        assertThat(eventData.m3u8Url).isNotEmpty
        assertThat(eventData.isLive).isTrue
        assertThat(eventData.isMuted).isTrue
        assertThat(eventData.streamFormat).isEqualTo("hls")
        assertThat(eventData.videoBitrate).isGreaterThan(0)
    }

    private fun verifyAnalyticsConfig(eventData: EventData, analyticsConfig: BitmovinAnalyticsConfig) {
        assertThat(eventData.videoTitle).isEqualTo(analyticsConfig.title)
        assertThat(eventData.videoId).isEqualTo(analyticsConfig.videoId)
        assertThat(eventData.cdnProvider).isEqualTo(analyticsConfig.cdnProvider)
        assertThat(eventData.customUserId).isEqualTo(analyticsConfig.customUserId)
        assertThat(eventData.experimentName).isEqualTo(analyticsConfig.experimentName)
        assertThat(eventData.path).isEqualTo(analyticsConfig.path)

        verifyCustomData(eventData, analyticsConfig)
    }

    private fun verifyCustomData(eventData: EventData, analyticsConfig: BitmovinAnalyticsConfig) {
        assertThat(eventData.customData1).isEqualTo(analyticsConfig.customData1)
        assertThat(eventData.customData2).isEqualTo(analyticsConfig.customData2)
        assertThat(eventData.customData3).isEqualTo(analyticsConfig.customData3)
        assertThat(eventData.customData4).isEqualTo(analyticsConfig.customData4)
        assertThat(eventData.customData5).isEqualTo(analyticsConfig.customData5)
        assertThat(eventData.customData6).isEqualTo(analyticsConfig.customData6)
        assertThat(eventData.customData7).isEqualTo(analyticsConfig.customData7)
    }
}
