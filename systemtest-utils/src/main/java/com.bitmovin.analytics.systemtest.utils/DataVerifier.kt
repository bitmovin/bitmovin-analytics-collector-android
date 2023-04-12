package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail

object DataVerifier {
    val QUALITYCHANGE = "qualitychange"
    val BUFFERING = "buffering"
    val STARTUP = "startup"
    val SEEKING = "seeking"

    // verifies properties that are not specific to playback order
    fun verifyStaticData(
        eventDataList: MutableList<EventData>,
        analyticsConfig: BitmovinAnalyticsConfig,
        expectedStreamData: StreamData,
        expectedPlayerInfo: PlayerInfo,
        is4kTV: Boolean = false,
    ) {
        if (eventDataList.size == 0) {
            fail<Nothing>("No eventData samples collected")
        }

        verifyStaticData(eventDataList, expectedStreamData, expectedPlayerInfo, is4kTV)

        for (eventData in eventDataList) {
            verifyAnalyticsConfig(eventData, analyticsConfig)
        }
    }

    fun verifyStaticData(
        eventDataList: MutableList<EventData>,
        sourceMetadata: SourceMetadata,
        expectedStreamData: StreamData,
        expectedPlayerInfo: PlayerInfo,
        is4kTV: Boolean = false,
    ) {
        if (eventDataList.size == 0) {
            fail<Nothing>("No eventData samples collected")
        }

        verifyStaticData(eventDataList, expectedStreamData, expectedPlayerInfo, is4kTV)

        for (eventData in eventDataList) {
            verifySourceMetadata(eventData, sourceMetadata)
        }
    }

    private fun verifyStaticData(
        eventDataList: MutableList<EventData>,
        expectedStreamData: StreamData,
        expectedPlayerInfo: PlayerInfo,
        is4kTV: Boolean,
    ) {
        // make sure that these properties are static over the whole session
        val generatedUserId = eventDataList[0].userId
        val impressionId = eventDataList[0].impressionId

        for ((expectedSequenceNumber, eventData) in eventDataList.withIndex()) {
            if (is4kTV) {
                verify4kTVDeviceInfo(eventData)
            } else {
                verifyPhoneDeviceInfo(eventData)
            }

            verifyPlayerAndCollectorInfo(eventData, expectedPlayerInfo)
            verifyStreamData(eventData, expectedStreamData)
            verifyUserAgent(eventData)

            assertThat(eventData.impressionId).isEqualTo(impressionId)
            assertThat(eventData.userId).isEqualTo(generatedUserId)
            assertThat(eventData.videoStartFailed).isFalse

            // dropped frames can never be negative
            // verified since IVS player behaves weird with the internal statistics
            assertThat(eventData.droppedFrames).isGreaterThanOrEqualTo(0)

            // make sure that sequenceNumber is continuous increasing
            assertThat(eventData.sequenceNumber).isEqualTo(expectedSequenceNumber)
        }
    }

    private fun verifyPlayerAndCollectorInfo(eventData: EventData, expectedPlayerInfo: PlayerInfo) {
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
        assertThat(eventData.videoCodec).startsWith(exepectedData.videoCodecStartsWith)
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

    fun verifySourceMetadata(eventData: EventData, sourceMetadata: SourceMetadata) {
        assertThat(eventData.videoTitle).isEqualTo(sourceMetadata.title)
        assertThat(eventData.videoId).isEqualTo(sourceMetadata.videoId)
        assertThat(eventData.cdnProvider).isEqualTo(sourceMetadata.cdnProvider)
        assertThat(eventData.experimentName).isEqualTo(sourceMetadata.experimentName)
        assertThat(eventData.path).isEqualTo(sourceMetadata.path)

        assertThat(eventData.customData1).isEqualTo(sourceMetadata.customData1)
        assertThat(eventData.customData2).isEqualTo(sourceMetadata.customData2)
        assertThat(eventData.customData3).isEqualTo(sourceMetadata.customData3)
        assertThat(eventData.customData4).isEqualTo(sourceMetadata.customData4)
        assertThat(eventData.customData5).isEqualTo(sourceMetadata.customData5)
        assertThat(eventData.customData6).isEqualTo(sourceMetadata.customData6)
        assertThat(eventData.customData7).isEqualTo(sourceMetadata.customData7)
    }

    private fun verifyPhoneDeviceInfo(eventData: EventData) {
        assertThat(eventData.deviceInformation.model).isNotEmpty
        assertThat(eventData.deviceInformation.isTV).isFalse
        assertThat(eventData.deviceInformation.manufacturer).isNotEmpty
        assertThat(eventData.screenWidth).isGreaterThan(0)
        assertThat(eventData.screenHeight).isGreaterThan(0)
        assertThat(eventData.platform).isEqualTo("android")
    }

    private fun verify4kTVDeviceInfo(eventData: EventData) {
        assertThat(eventData.deviceInformation.model).isNotEmpty
        assertThat(eventData.deviceInformation.isTV).isTrue
        assertThat(eventData.deviceInformation.manufacturer).isNotEmpty
        assertThat(eventData.screenWidth).isEqualTo(3840)
        assertThat(eventData.screenHeight).isEqualTo(2160)
        assertThat(eventData.platform).isEqualTo("androidTV")
    }

    private fun verifyUserAgent(eventData: EventData) {
        assertThat(eventData.userAgent).isNotEmpty
        assertThat(eventData.userAgent).contains("Android 1") // is dynamic so we only check that it is at least Android 1x
    }

    fun verifyStartupSample(eventData: EventData, isFirstImpression: Boolean = true) {
        assertThat(eventData.state).isEqualTo(STARTUP)
        assertThat(eventData.startupTime).isGreaterThan(0)
        assertThat(eventData.supportedVideoCodecs).isNotNull

        // if user watches several different videos there are several impressions but only the first one has playerStartupTime !=0
        if (isFirstImpression) {
            assertThat(eventData.playerStartupTime).isEqualTo(1)
        }
        assertThat(eventData.videoStartupTime).isGreaterThan(0)
        assertThat(eventData.videoTimeStart).isEqualTo(0)
        assertThat(eventData.videoTimeEnd).isEqualTo(0)
        assertThat(eventData.droppedFrames).isEqualTo(0)
    }

    fun verifyHasNoErrorSamples(impression: Impression) {
        assertThat(impression.errorDetailList.size).isEqualTo(0)

        val errorSamples = impression.eventDataList.filter { x ->
            x.errorMessage != null ||
                x.errorData != null ||
                x.errorCode != null
        }

        assertThat(errorSamples.size).isEqualTo(0)
    }

    fun verifyQualityOnlyChangesWithQualityChangeEventOrSeek(eventDataList: MutableList<EventData>) {
        var currentVideoBitrate = eventDataList[0].videoBitrate
        var currentAudioBitrate = eventDataList[0].audioBitrate

        for (eventData in eventDataList) {
            if (eventData.state == QUALITYCHANGE || eventData.state == SEEKING) {
                currentVideoBitrate = eventData.videoBitrate
                currentAudioBitrate = eventData.audioBitrate
            }

            if (eventData.videoBitrate != currentVideoBitrate) {
                Assertions.fail<Nothing>("video quality changed before qualitychangeevent")
            }

            if (eventData.audioBitrate != currentAudioBitrate) {
                Assertions.fail<Nothing>("audio quality changed before qualitychangeevent")
            }
        }
    }

    fun verifyVideoStartEndTimesOnContinuousPlayback(eventDataList: MutableList<EventData>) {
        // startup sample should just have the same videoStart and videoEnd, thus we init with videoTimeEnd from first sample
        var previousVideoTimeEnd = eventDataList[0].videoTimeEnd

        for (eventData in eventDataList) {
            if (eventData.state != "seeking") { // on seeking we might not have monotonic increasing videostart and videoend

                // we need to add a couple of ms to videoTimeEnd to make test stable
                // since it seems like ivs player is sometimes changing the position backwards a bit on
                // subsequent player.position calls after a seek, which affects the playing sample after the seek
                assertThat(eventData.videoTimeStart).isLessThanOrEqualTo(eventData.videoTimeEnd + 5)
            }
            assertThat(eventData.videoTimeStart).isEqualTo(previousVideoTimeEnd)
            previousVideoTimeEnd = eventData.videoTimeEnd
        }
    }

    fun verifyStaticErrorDetails(errorDetail: ErrorDetail, expectedImpressionId: String, expectedLicenseKey: String) {
        assertThat(errorDetail.impressionId).isEqualTo(expectedImpressionId)
        assertThat(errorDetail.platform).isEqualTo("android")
        assertThat(errorDetail.licenseKey).isEqualTo(expectedLicenseKey)
        assertThat(errorDetail.analyticsVersion).isEqualTo("0.0.0-local")
        assertThat(errorDetail.timestamp).isGreaterThan(0)
        assertThat(errorDetail.domain).isNotEmpty
    }

    fun verifyExactlyOneSeekingSample(eventDataList: MutableList<EventData>) {
        verifyOnlyOneSampleHasState(eventDataList, "seeking")
    }

    fun verifyExactlyOnPauseSample(eventDataList: MutableList<EventData>) {
        verifyOnlyOneSampleHasState(eventDataList, "pause")
    }

    private fun verifyOnlyOneSampleHasState(eventDataList: MutableList<EventData>, state: String) {
        val seekingSamples = eventDataList.filter { x -> x.state?.lowercase() == state }
        assertThat(seekingSamples.size).isEqualTo(1)
    }
}
