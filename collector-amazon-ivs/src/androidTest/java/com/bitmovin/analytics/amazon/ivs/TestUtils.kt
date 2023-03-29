package com.bitmovin.analytics.amazon.ivs

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.EventData
import org.assertj.core.api.Assertions.assertThat

object TestUtils {

    fun createBitmovinAnalyticsConfig(m3u8Url: String): BitmovinAnalyticsConfig {
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
        bitmovinAnalyticsConfig.m3u8Url = m3u8Url
        bitmovinAnalyticsConfig.cdnProvider = "testCdnProvider"
        return bitmovinAnalyticsConfig
    }

    fun verifyPlayerAndCollectorInfo(eventData: EventData, expectedPlayerInfo: PlayerInfo) {
        assertThat(eventData.player).isEqualTo(expectedPlayerInfo.playerName)
        assertThat(eventData.playerTech).isEqualTo(expectedPlayerInfo.playerTech)
        assertThat(eventData.version).isEqualTo(expectedPlayerInfo.playerVersion)

        assertThat(eventData.analyticsVersion).isEqualTo("0.0.0-local")
    }

    fun verifyPlayerSetting(eventData: EventData, expectedPlayerSettings: PlayerSettings) {
        assertThat(eventData.isMuted).isEqualTo(expectedPlayerSettings.isMuted)
    }

    fun verifyStreamData(eventData: EventData, exepectedData: StreamData) {
        assertThat(eventData.audioCodec).isEqualTo(exepectedData.audioCodec)
        assertThat(eventData.videoCodec).isEqualTo(exepectedData.videoCodec)
        assertThat(eventData.m3u8Url).isEqualTo(exepectedData.m3u8Url)
        assertThat(eventData.streamFormat).isEqualTo(exepectedData.streamFormat)
        assertThat(eventData.videoBitrate).isGreaterThan(0)
        assertThat(eventData.isLive).isEqualTo(exepectedData.isLive)
        assertThat(eventData.videoDuration).isEqualTo(exepectedData.duration)
    }

    fun verifyAnalyticsConfig(eventData: EventData, analyticsConfig: BitmovinAnalyticsConfig) {
        assertThat(eventData.videoTitle).isEqualTo(analyticsConfig.title)
        assertThat(eventData.videoId).isEqualTo(analyticsConfig.videoId)
        assertThat(eventData.cdnProvider).isEqualTo(analyticsConfig.cdnProvider)
        assertThat(eventData.customUserId).isEqualTo(analyticsConfig.customUserId)
        assertThat(eventData.experimentName).isEqualTo(analyticsConfig.experimentName)
        assertThat(eventData.path).isEqualTo(analyticsConfig.path)

        assertThat(eventData.customData1).isEqualTo(analyticsConfig.customData1)
        assertThat(eventData.customData2).isEqualTo(analyticsConfig.customData2)
        assertThat(eventData.customData3).isEqualTo(analyticsConfig.customData3)
        assertThat(eventData.customData4).isEqualTo(analyticsConfig.customData4)
        assertThat(eventData.customData5).isEqualTo(analyticsConfig.customData5)
        assertThat(eventData.customData6).isEqualTo(analyticsConfig.customData6)
        assertThat(eventData.customData7).isEqualTo(analyticsConfig.customData7)
    }

    fun verifyPhoneDeviceInfo(eventData: EventData) {
        assertThat(eventData.deviceInformation.model).isNotEmpty
        assertThat(eventData.deviceInformation.isTV).isFalse
        assertThat(eventData.deviceInformation.manufacturer).isNotEmpty
        assertThat(eventData.screenWidth).isGreaterThan(0)
        assertThat(eventData.screenHeight).isGreaterThan(0)
        assertThat(eventData.platform).isEqualTo("android")
    }

    fun verify4kTVDeviceInfo(eventData: EventData) {
        assertThat(eventData.deviceInformation.model).isNotEmpty
        assertThat(eventData.deviceInformation.isTV).isTrue
        assertThat(eventData.deviceInformation.manufacturer).isNotEmpty
        assertThat(eventData.screenWidth).isEqualTo(3840)
        assertThat(eventData.screenHeight).isEqualTo(2160)
        assertThat(eventData.platform).isEqualTo("androidTV")
    }

    fun verifyUserAgent(eventData: EventData) {
        assertThat(eventData.userAgent).isNotEmpty
        assertThat(eventData.userAgent).contains("Android 1") // is dynamic so we only check that it is at least Android 1x
    }

    fun filterNonDeterministicEvents(eventDataList: MutableList<EventData>) {
        // We filter for qualitychange and buffering events
        // since they are non deterministic and would probably make the test flaky
        eventDataList.removeAll { x -> x.state?.lowercase() == "qualitychange" }
        eventDataList.removeAll { x -> x.state?.lowercase() == "buffering" }
    }

    fun verifyIvsPlayerStartupSample(eventData: EventData) {
        assertThat(eventData.state).isEqualTo("startup")
        assertThat(eventData.startupTime).isGreaterThan(0)
        assertThat(eventData.supportedVideoCodecs).isNotNull // TODO: check if this is static in ivs
        assertThat(eventData.playerStartupTime).isEqualTo(1)
        assertThat(eventData.videoStartupTime).isGreaterThan(0)
    }

    fun verifyDroppedFramesAreNeverNegative(eventDataList: MutableList<EventData>) {
        val negativeDroppedFramesSamples = eventDataList.filter { x -> x.droppedFrames < 0 }
        assertThat(negativeDroppedFramesSamples.size).isEqualTo(0)
    }
}
