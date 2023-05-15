package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.CustomData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail

object DataVerifier {
    val QUALITYCHANGE = "qualitychange"
    val BUFFERING = "buffering"
    val STARTUP = "startup"
    val SEEKING = "seeking"
    val PLAYING = "playing"
    val PAUSE = "pause"

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
        // verify that there is only one startup sample per session
        assertThat(eventDataList.filter { x -> x.state == STARTUP }.size).isEqualTo(1)

        // make sure that these properties are static over the whole session
        val generatedUserId = eventDataList[0].userId
        val impressionId = eventDataList[0].impressionId
        assertThat(impressionId).isNotBlank
        assertThat(generatedUserId).isNotBlank

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
            assertThat(eventData.droppedFrames).isGreaterThanOrEqualTo(0)
            assertThat(eventData.language).isEqualTo("en_US")

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

    fun verifyPlayerSetting(eventDataList: MutableList<EventData>, expectedPlayerSettings: PlayerSettings) {
        for (eventData in eventDataList) {
            assertThat(eventData.isMuted).isEqualTo(expectedPlayerSettings.isMuted)
        }
    }

    fun verifyStreamData(eventData: EventData, expectedData: StreamData) {
        assertThat(eventData.audioCodec).isEqualTo(expectedData.audioCodec)
        assertThat(eventData.videoCodec).startsWith(expectedData.videoCodecStartsWith)
        assertThat(eventData.m3u8Url).isEqualTo(expectedData.m3u8Url)
        assertThat(eventData.progUrl).isEqualTo(expectedData.progUrl)
        assertThat(eventData.mpdUrl).isEqualTo(expectedData.mpdUrl)
        assertThat(eventData.streamFormat).isEqualTo(expectedData.streamFormat)

        // if video is progressive, bitrate is set to -1 (TODO: verify why?)
        if (expectedData.progUrl != null) {
            assertThat(eventData.videoBitrate).isEqualTo(-1)
        } else {
            assertThat(eventData.videoBitrate).isGreaterThan(0)
        }

        assertThat(eventData.isLive).isEqualTo(expectedData.isLive)
        assertThat(eventData.videoDuration).isEqualTo(expectedData.duration)

        assertThat(eventData.videoPlaybackHeight).isGreaterThan(0)
        assertThat(eventData.videoPlaybackWidth).isGreaterThan(8)
    }

    fun verifyAnalyticsConfig(eventData: List<EventData>, analyticsConfig: BitmovinAnalyticsConfig) {
        for (event in eventData) {
            verifyAnalyticsConfig(event, analyticsConfig)
        }
    }

    fun verifyAnalyticsConfig(eventData: EventData, analyticsConfig: BitmovinAnalyticsConfig) {
        assertThat(eventData.videoTitle).isEqualTo(analyticsConfig.title)
        assertThat(eventData.videoId).isEqualTo(analyticsConfig.videoId)
        assertThat(eventData.cdnProvider).isEqualTo(analyticsConfig.cdnProvider)
        assertThat(eventData.customUserId).isEqualTo(analyticsConfig.customUserId)
        assertThat(eventData.path).isEqualTo(analyticsConfig.path)

        // assert all customDatafields from config
        assertThat(eventData.experimentName).isEqualTo(analyticsConfig.experimentName)
        assertThat(eventData.customData1).isEqualTo(analyticsConfig.customData1)
        assertThat(eventData.customData2).isEqualTo(analyticsConfig.customData2)
        assertThat(eventData.customData3).isEqualTo(analyticsConfig.customData3)
        assertThat(eventData.customData4).isEqualTo(analyticsConfig.customData4)
        assertThat(eventData.customData5).isEqualTo(analyticsConfig.customData5)
        assertThat(eventData.customData6).isEqualTo(analyticsConfig.customData6)
        assertThat(eventData.customData7).isEqualTo(analyticsConfig.customData7)
        assertThat(eventData.customData8).isEqualTo(analyticsConfig.customData8)
        assertThat(eventData.customData9).isEqualTo(analyticsConfig.customData9)
        assertThat(eventData.customData10).isEqualTo(analyticsConfig.customData10)
        assertThat(eventData.customData11).isEqualTo(analyticsConfig.customData11)
        assertThat(eventData.customData12).isEqualTo(analyticsConfig.customData12)
        assertThat(eventData.customData13).isEqualTo(analyticsConfig.customData13)
        assertThat(eventData.customData14).isEqualTo(analyticsConfig.customData14)
        assertThat(eventData.customData15).isEqualTo(analyticsConfig.customData15)
        assertThat(eventData.customData16).isEqualTo(analyticsConfig.customData16)
        assertThat(eventData.customData17).isEqualTo(analyticsConfig.customData17)
        assertThat(eventData.customData18).isEqualTo(analyticsConfig.customData18)
        assertThat(eventData.customData19).isEqualTo(analyticsConfig.customData19)
        assertThat(eventData.customData20).isEqualTo(analyticsConfig.customData20)
        assertThat(eventData.customData21).isEqualTo(analyticsConfig.customData21)
        assertThat(eventData.customData22).isEqualTo(analyticsConfig.customData22)
        assertThat(eventData.customData23).isEqualTo(analyticsConfig.customData23)
        assertThat(eventData.customData24).isEqualTo(analyticsConfig.customData24)
        assertThat(eventData.customData25).isEqualTo(analyticsConfig.customData25)
        assertThat(eventData.customData26).isEqualTo(analyticsConfig.customData26)
        assertThat(eventData.customData27).isEqualTo(analyticsConfig.customData27)
        assertThat(eventData.customData28).isEqualTo(analyticsConfig.customData28)
        assertThat(eventData.customData29).isEqualTo(analyticsConfig.customData29)
        assertThat(eventData.customData30).isEqualTo(analyticsConfig.customData30)
    }

    fun verifyCustomData(eventDataList: List<EventData>, customData: CustomData) {
        for (eventData in eventDataList) {
            verifyCustomData(eventData, customData)
        }
    }

    fun verifyCustomData(eventData: EventData, customData: CustomData) {
        assertThat(eventData.experimentName).isEqualTo(customData.experimentName)
        assertThat(eventData.customData1).isEqualTo(customData.customData1)
        assertThat(eventData.customData2).isEqualTo(customData.customData2)
        assertThat(eventData.customData3).isEqualTo(customData.customData3)
        assertThat(eventData.customData4).isEqualTo(customData.customData4)
        assertThat(eventData.customData5).isEqualTo(customData.customData5)
        assertThat(eventData.customData6).isEqualTo(customData.customData6)
        assertThat(eventData.customData7).isEqualTo(customData.customData7)
        assertThat(eventData.customData8).isEqualTo(customData.customData8)
        assertThat(eventData.customData9).isEqualTo(customData.customData9)
        assertThat(eventData.customData10).isEqualTo(customData.customData10)
        assertThat(eventData.customData11).isEqualTo(customData.customData11)
        assertThat(eventData.customData12).isEqualTo(customData.customData12)
        assertThat(eventData.customData13).isEqualTo(customData.customData13)
        assertThat(eventData.customData14).isEqualTo(customData.customData14)
        assertThat(eventData.customData15).isEqualTo(customData.customData15)
        assertThat(eventData.customData16).isEqualTo(customData.customData16)
        assertThat(eventData.customData17).isEqualTo(customData.customData17)
        assertThat(eventData.customData18).isEqualTo(customData.customData18)
        assertThat(eventData.customData19).isEqualTo(customData.customData19)
        assertThat(eventData.customData20).isEqualTo(customData.customData20)
        assertThat(eventData.customData21).isEqualTo(customData.customData21)
        assertThat(eventData.customData22).isEqualTo(customData.customData22)
        assertThat(eventData.customData23).isEqualTo(customData.customData23)
        assertThat(eventData.customData24).isEqualTo(customData.customData24)
        assertThat(eventData.customData25).isEqualTo(customData.customData25)
        assertThat(eventData.customData26).isEqualTo(customData.customData26)
        assertThat(eventData.customData27).isEqualTo(customData.customData27)
        assertThat(eventData.customData28).isEqualTo(customData.customData28)
        assertThat(eventData.customData29).isEqualTo(customData.customData29)
        assertThat(eventData.customData30).isEqualTo(customData.customData30)
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
        //   assertThat(eventData.videoTimeEnd).isEqualTo(0) // we can end up with startup samples that have non 0 videoTimeEnd, this needs to be investigated
        assertThat(eventData.droppedFrames).isEqualTo(0)
        assertThat(eventData.sequenceNumber).isEqualTo(0)
    }

    fun verifyDrmStartupSample(eventData: EventData, drmType: String?, isFirstImpression: Boolean = true, isAutoPlay: Boolean = true) {
        verifyStartupSample(eventData, isFirstImpression)
        assertThat(eventData.drmType).isEqualTo(drmType)
        assertThat(eventData.drmLoadTime).isGreaterThan(0)

        if (isAutoPlay) {
            assertThat(eventData.drmLoadTime).isLessThan(eventData.videoStartupTime)
        }
    }

    fun verifyStartupSampleOnError(eventData: EventData, expectedPlayerInfo: PlayerInfo) {
        assertThat(eventData.state).isIn("startup", "ready") // we are ending up with ready state on exoplayer and ivs
        assertThat(eventData.videoStartupTime).isEqualTo(0)
        assertThat(eventData.videoTimeStart).isEqualTo(0)
        assertThat(eventData.videoTimeEnd).isEqualTo(0)
        assertThat(eventData.droppedFrames).isEqualTo(0)
        assertThat(eventData.videoStartFailed).isTrue
        assertThat(eventData.language).isEqualTo("en_US")
        assertThat(eventData.impressionId).isNotBlank
        assertThat(eventData.userId).isNotBlank
        assertThat(eventData.sequenceNumber).isEqualTo(0)
        assertThat(eventData.domain).isNotBlank

        verifyPhoneDeviceInfo(eventData)
        verifyPlayerAndCollectorInfo(eventData, expectedPlayerInfo)
        verifyUserAgent(eventData)
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

    fun verifyVideoStartEndTimesOnContinuousPlayback(eventDataList: MutableList<EventData>) {
        // TODO: we skip the startup sample here, since the videotime start and videotime end are sometimes not 0 for these (seen on bitmovin player)
        var previousVideoTimeEnd = eventDataList[0].videoTimeEnd
        var previousWasAd = false

        for (eventData in eventDataList.subList(1, eventDataList.size)) {
            if (eventData.state != "seeking") { // on seeking we might not have monotonic increasing videostart and videoend

                // we need to add a couple of ms to videoTimeEnd to make test stable
                // since it seems like ivs player is sometimes changing the position backwards a bit on
                // subsequent player.position calls after a seek, which affects the playing sample after the seek
                assertThat(eventData.videoTimeStart).isLessThanOrEqualTo(eventData.videoTimeEnd + 5)
            }

            // we don't check for continous playback in case ad was played before
            // since ads have startTime = endTime, but first sample after ad, has startTime that is a
            // bit higher than endTime of ad
            if (!previousWasAd) {
                assertThat(eventData.videoTimeStart).isEqualTo(previousVideoTimeEnd)
            }

            previousVideoTimeEnd = eventData.videoTimeEnd
            previousWasAd = eventData.ad == 1
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

    fun verifyInvariants(eventDataList: MutableList<EventData>) {
        verifyQualityOnlyChangesWithQualityChangeEventOrSeek(eventDataList)
        verifyStateDurationsAreSetCorrectly(eventDataList)

        // ivs is reporting videotime inconsistently for live samples, thus we skip this check for ivs live
        // TODO: we also skip the check for the bitmovin player, since tests are unstable right now, needs to be investigated
        // how we can improve the accuracy here.
        if (!(eventDataList[0].isLive && eventDataList[0].player == "amazonivs") &&
            eventDataList[0].player != "bitmovin"
        ) {
            verifyPlayingDurationCorrelatesWithVideoTimeStartAndEnd(eventDataList)
        }
    }

    fun verifySubtitles(eventDataList: MutableList<EventData>, enabled: Boolean = false, language: String? = null) {
        for (eventData in eventDataList) {
            assertThat(eventData.subtitleEnabled).isEqualTo(enabled)
            assertThat(eventData.subtitleLanguage).isEqualTo(language)
        }
    }

    fun verifyThereWasExactlyOneSeekingSample(eventDataList: MutableList<EventData>) {
        verifyOnlyOneSampleHasState(eventDataList, SEEKING)
    }

    fun verifyExactlyOnePauseSample(eventDataList: MutableList<EventData>) {
        verifyOnlyOneSampleHasState(eventDataList, PAUSE)
    }

    fun verifyThereWasAtLeastOnePlayingSample(eventDataList: MutableList<EventData>) {
        verifyAtLeastOneSampleHasState(eventDataList, PLAYING)
    }

    private fun verifyOnlyOneSampleHasState(eventDataList: MutableList<EventData>, state: String) {
        val samplesWithState = eventDataList.filter { x -> x.state?.lowercase() == state }
        assertThat(samplesWithState.size).isEqualTo(1)
    }

    private fun verifyAtLeastOneSampleHasState(eventDataList: MutableList<EventData>, state: String) {
        val samplesWithState = eventDataList.filter { x -> x.state?.lowercase() == state }
        assertThat(samplesWithState.size).isGreaterThanOrEqualTo(1)
    }

    private fun verifyStateDurationsAreSetCorrectly(eventDataList: MutableList<EventData>) {
        val playingSamples = eventDataList.filter { x -> x.state?.lowercase() == PLAYING }
        playingSamples.forEach {
            assertThat(it.duration).isEqualTo(it.played)
        }

        val seekSamples = eventDataList.filter { x -> x.state?.lowercase() == SEEKING }
        seekSamples.forEach {
            assertThat(it.duration).isEqualTo(it.seeked)
        }

        val pauseSamples = eventDataList.filter { x -> x.state?.lowercase() == PAUSE }
        pauseSamples.forEach {
            assertThat(it.duration).isEqualTo(it.paused)
        }
    }

    private fun verifyQualityOnlyChangesWithQualityChangeEventOrSeek(eventDataList: MutableList<EventData>) {
        var currentVideoBitrate = eventDataList[0].videoBitrate
        var currentAudioBitrate = eventDataList[0].audioBitrate

        for (eventData in eventDataList) {
            if (eventData.state == QUALITYCHANGE || eventData.state == SEEKING) {
                currentVideoBitrate = eventData.videoBitrate
                currentAudioBitrate = eventData.audioBitrate
            }

            if (eventData.videoBitrate != currentVideoBitrate) {
                fail<Nothing>("video quality changed before qualitychangeevent")
            }

            if (eventData.audioBitrate != currentAudioBitrate) {
                fail<Nothing>("audio quality changed before qualitychangeevent")
            }
        }
    }

    private fun verifyPlayingDurationCorrelatesWithVideoTimeStartAndEnd(eventDataList: MutableList<EventData>) {
        val playingEvents = eventDataList.filter { x -> x.state == PLAYING }
        val playingStartEndDelta = playingEvents.sumOf { it.videoTimeEnd - it.videoTimeStart }
        val playingDuration = playingEvents.sumOf { it.played }

        // we use a range of -5% to +10% to account for some inaccuracies in the players
        assertThat(playingStartEndDelta).isBetween((playingDuration * 0.95).toLong(), (playingDuration * 1.10).toLong())
    }
}
