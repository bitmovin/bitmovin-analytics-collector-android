package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
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
        expectedSourceMetadata: SourceMetadata,
        expectedStreamData: StreamData,
        expectedPlayerInfo: PlayerInfo,
        is4kTV: Boolean = false,
        expectedCustomUserId: String? = null,
    ) {
        if (eventDataList.size == 0) {
            fail<Nothing>("No eventData samples collected")
        }

        verifyStaticData(eventDataList, expectedStreamData, expectedPlayerInfo, is4kTV)

        for (eventData in eventDataList) {
            verifySourceMetadata(eventData, expectedSourceMetadata)
            assertThat(eventData.customUserId).isEqualTo(expectedCustomUserId)
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

            // audio language should always be set, except for ivs player
            // since we cannot track it there as of 2023-09-21
            if (expectedPlayerInfo.playerName != "amazonivs") {
                assertThat(eventData.audioLanguage).isNotEmpty
            }

            // make sure that sequenceNumber is continuous increasing
            assertThat(eventData.sequenceNumber).isEqualTo(expectedSequenceNumber)
        }
    }

    private fun verifyPlayerAndCollectorInfo(
        eventData: EventData,
        expectedPlayerInfo: PlayerInfo,
    ) {
        assertThat(eventData.player).isEqualTo(expectedPlayerInfo.playerName)
        assertThat(eventData.playerTech).isEqualTo(expectedPlayerInfo.playerTech)
        assertThat(eventData.version).isEqualTo(expectedPlayerInfo.playerVersion)

        assertThat(eventData.analyticsVersion).isEqualTo("0.0.0-local")
    }

    fun verifyPlayerSetting(
        eventDataList: MutableList<EventData>,
        expectedPlayerSettings: PlayerSettings,
    ) {
        for (eventData in eventDataList) {
            assertThat(eventData.isMuted).isEqualTo(expectedPlayerSettings.isMuted)
        }

        // Autoplay should only be included in the Startup packets
        assertThat(EventDataUtils.getStartupEvent(eventDataList).autoplay).isEqualTo(expectedPlayerSettings.isAutoPlayEnabled)

        val nonStartupEvents = eventDataList.filter { it.state != STARTUP }
        for (eventData in nonStartupEvents) {
            assertThat(eventData.autoplay).isEqualTo(null)
        }
    }

    fun verifyStreamData(
        eventData: EventData,
        expectedData: StreamData,
    ) {
        assertThat(eventData.audioCodec).isEqualTo(expectedData.audioCodec)
        assertThat(eventData.videoCodec).startsWith(expectedData.videoCodecStartsWith)
        assertThat(eventData.streamFormat).isEqualTo(expectedData.streamFormat)

        if (expectedData.progUrl != null) {
            // TODO
            // We don't do any checks on videoBitrate when progressive source is used
            // since it seems to be a bit flaky,
            // bitrate is set to -1 mostly but sometimes to 4800000
        } else {
            assertThat(eventData.videoBitrate).isGreaterThan(0)
        }

        assertThat(eventData.isLive).isEqualTo(expectedData.isLive)
        assertThat(eventData.videoDuration).isEqualTo(expectedData.duration)

        assertThat(eventData.videoPlaybackHeight).isGreaterThan(0)
        assertThat(eventData.videoPlaybackWidth).isGreaterThan(0)

        // autodection of source urls only works on bitmovin player and exoplayer
        if (eventData.player != "amazonivs") {
            assertThat(eventData.mpdUrl).isEqualTo(expectedData.mpdUrl)
            assertThat(eventData.m3u8Url).isEqualTo(expectedData.m3u8Url)

            // (on exoplayer progressive cannot be detected right now)
            if (eventData.player != "exoplayer") {
                assertThat(eventData.progUrl).isEqualTo(expectedData.progUrl)
            }
        }
    }

    fun verifyAnalyticsConfig(
        eventData: List<EventData>,
        analyticsConfig: BitmovinAnalyticsConfig,
    ) {
        for (event in eventData) {
            verifyAnalyticsConfig(event, analyticsConfig)
        }
    }

    fun verifyAnalyticsConfig(
        eventData: EventData,
        analyticsConfig: BitmovinAnalyticsConfig,
    ) {
        assertThat(eventData.videoTitle).isEqualTo(analyticsConfig.title)
        assertThat(eventData.videoId).isEqualTo(analyticsConfig.videoId)
        assertThat(eventData.cdnProvider).isEqualTo(analyticsConfig.cdnProvider)
        assertThat(eventData.customUserId).isEqualTo(analyticsConfig.customUserId)
        assertThat(eventData.path).isEqualTo(analyticsConfig.path)
        assertThat(eventData.experimentName).isEqualTo(analyticsConfig.experimentName)

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

    fun verifyCustomData(
        eventDataList: List<EventData>,
        customData: CustomData,
    ) {
        for (eventData in eventDataList) {
            verifyCustomData(eventData, customData)
        }
    }

    fun verifyCustomData(
        eventData: EventData,
        expectedCustomData: CustomData,
    ) {
        assertThat(eventData.customData1).isEqualTo(expectedCustomData.customData1)
        assertThat(eventData.customData2).isEqualTo(expectedCustomData.customData2)
        assertThat(eventData.customData3).isEqualTo(expectedCustomData.customData3)
        assertThat(eventData.customData4).isEqualTo(expectedCustomData.customData4)
        assertThat(eventData.customData5).isEqualTo(expectedCustomData.customData5)
        assertThat(eventData.customData6).isEqualTo(expectedCustomData.customData6)
        assertThat(eventData.customData7).isEqualTo(expectedCustomData.customData7)
        assertThat(eventData.customData8).isEqualTo(expectedCustomData.customData8)
        assertThat(eventData.customData9).isEqualTo(expectedCustomData.customData9)
        assertThat(eventData.customData10).isEqualTo(expectedCustomData.customData10)
        assertThat(eventData.customData11).isEqualTo(expectedCustomData.customData11)
        assertThat(eventData.customData12).isEqualTo(expectedCustomData.customData12)
        assertThat(eventData.customData13).isEqualTo(expectedCustomData.customData13)
        assertThat(eventData.customData14).isEqualTo(expectedCustomData.customData14)
        assertThat(eventData.customData15).isEqualTo(expectedCustomData.customData15)
        assertThat(eventData.customData16).isEqualTo(expectedCustomData.customData16)
        assertThat(eventData.customData17).isEqualTo(expectedCustomData.customData17)
        assertThat(eventData.customData18).isEqualTo(expectedCustomData.customData18)
        assertThat(eventData.customData19).isEqualTo(expectedCustomData.customData19)
        assertThat(eventData.customData20).isEqualTo(expectedCustomData.customData20)
        assertThat(eventData.customData21).isEqualTo(expectedCustomData.customData21)
        assertThat(eventData.customData22).isEqualTo(expectedCustomData.customData22)
        assertThat(eventData.customData23).isEqualTo(expectedCustomData.customData23)
        assertThat(eventData.customData24).isEqualTo(expectedCustomData.customData24)
        assertThat(eventData.customData25).isEqualTo(expectedCustomData.customData25)
        assertThat(eventData.customData26).isEqualTo(expectedCustomData.customData26)
        assertThat(eventData.customData27).isEqualTo(expectedCustomData.customData27)
        assertThat(eventData.customData28).isEqualTo(expectedCustomData.customData28)
        assertThat(eventData.customData29).isEqualTo(expectedCustomData.customData29)
        assertThat(eventData.customData30).isEqualTo(expectedCustomData.customData30)
        assertThat(eventData.experimentName).isEqualTo(expectedCustomData.experimentName)
    }

    fun verifySourceMetadata(
        eventData: EventData,
        sourceMetadata: SourceMetadata,
    ) {
        assertThat(eventData.videoTitle).isEqualTo(sourceMetadata.title)
        assertThat(eventData.videoId).isEqualTo(sourceMetadata.videoId)
        assertThat(eventData.cdnProvider).isEqualTo(sourceMetadata.cdnProvider)
        assertThat(eventData.path).isEqualTo(sourceMetadata.path)
        verifyCustomData(eventData, sourceMetadata.customData)
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

    fun verifyStartupSample(
        eventData: EventData,
        isFirstImpression: Boolean = true,
        expectedSequenceNumber: Int = 0,
    ) {
        assertThat(eventData.state).isEqualTo(STARTUP)
        assertThat(eventData.startupTime).isGreaterThan(0)

        // tests are using API level 30 (thus we can assume that all of the codecs below need to be supported)
        assertThat(eventData.supportedVideoCodecs).containsAll(listOf("av1", "avc", "hevc", "vp9"))

        // if user watches several different videos there are several impressions but only the first one has playerStartupTime !=0
        if (isFirstImpression) {
            assertThat(eventData.playerStartupTime).isEqualTo(1)
        }
        assertThat(eventData.videoStartupTime).isGreaterThan(0)
        assertThat(eventData.videoTimeStart).isEqualTo(0)
        //   assertThat(eventData.videoTimeEnd).isEqualTo(0) // we can end up with startup samples that have non 0 videoTimeEnd, this needs to be investigated
        assertThat(eventData.droppedFrames).isEqualTo(0)
        assertThat(eventData.sequenceNumber).isEqualTo(expectedSequenceNumber)
    }

    fun verifyDrmStartupSample(
        eventData: EventData,
        drmType: String?,
        isFirstImpression: Boolean = true,
        isAutoPlay: Boolean = true,
    ) {
        verifyStartupSample(eventData, isFirstImpression)
        assertThat(eventData.drmType).isEqualTo(drmType)
        assertThat(eventData.drmLoadTime).isGreaterThan(0)

        if (isAutoPlay) {
            assertThat(eventData.drmLoadTime).isLessThan(eventData.videoStartupTime)
        } else {
            // since we wait until player is ready, drmLoadTime should be greater
            // than videoStartupTime. Drm load time is the actual request time
            // and videoStartupTime the perceived time of the user
            assertThat(eventData.drmLoadTime).isGreaterThan(eventData.videoStartupTime)
        }
    }

    fun verifyStartupSampleOnError(
        eventData: EventData,
        expectedPlayerInfo: PlayerInfo,
    ) {
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

        val errorSamples =
            impression.eventDataList.filter { x ->
                x.errorMessage != null ||
                    x.errorData != null ||
                    x.errorCode != null
            }

        assertThat(errorSamples.size).isEqualTo(0)
    }

    fun verifyBackwardsSeek(eventData: EventData) {
        assertThat(eventData.videoTimeEnd).isLessThan(eventData.videoTimeStart)
        verifySeekRelatedFields(eventData)
    }

    fun verifyForwardsSeek(eventData: EventData) {
        assertThat(eventData.videoTimeStart).isLessThan(eventData.videoTimeEnd)
        verifySeekRelatedFields(eventData)
    }

    private fun verifySeekRelatedFields(eventData: EventData) {
        assertThat(eventData.seeked).isGreaterThan(0L)
        assertThat(eventData.duration).isGreaterThan(0L)
        assertThat(eventData.duration).isEqualTo(eventData.seeked)

        assertThat(eventData.played).isEqualTo(0)
        assertThat(eventData.paused).isEqualTo(0)
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
                assertThat(eventData.videoTimeStart).isLessThanOrEqualTo(eventData.videoTimeEnd + 30)
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

    fun verifyStaticErrorDetails(
        errorDetail: ErrorDetail,
        expectedImpressionId: String,
        expectedLicenseKey: String,
    ) {
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

    fun verifySubtitles(
        eventDataList: MutableList<EventData>,
        enabled: Boolean = false,
        language: String? = null,
    ) {
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

    fun verifyMpdSourceUrl(
        eventDataList: MutableList<EventData>,
        expectedMpdSourceUrl: String,
    ) {
        for (eventData in eventDataList) {
            assertThat(eventData.mpdUrl).isEqualTo(expectedMpdSourceUrl)
        }
    }

    fun verifyIsPlayingEvent(eventData: EventData) {
        assertThat(eventData.state).isEqualTo(PLAYING)
        assertThat(eventData.played).isGreaterThan(0)
        assertThat(eventData.duration).isEqualTo(eventData.played)
        assertThat(eventData.paused).isEqualTo(0)
        assertThat(eventData.seeked).isEqualTo(0)
    }

    fun verifyM3u8SourceUrl(
        eventDataList: MutableList<EventData>,
        expectedM3u8SourceUrl: String,
    ) {
        for (eventData in eventDataList) {
            assertThat(eventData.m3u8Url).isEqualTo(expectedM3u8SourceUrl)
        }
    }

    fun verifyProgSourceUrl(
        eventDataList: MutableList<EventData>,
        expectedProgSourceUrl: String,
    ) {
        for (eventData in eventDataList) {
            assertThat(eventData.progUrl).isEqualTo(expectedProgSourceUrl)
        }
    }

    fun getSsaiAdSamplesByIndex(
        eventDataList: MutableList<EventData>,
        adIndex: Int,
    ): MutableList<EventData> {
        val startIndex = eventDataList.indexOfFirst { it.adIndex == adIndex }
        if (startIndex == -1) return mutableListOf()

        val endIndex =
            eventDataList.subList(startIndex, eventDataList.size).indexOfFirst {
                it.adIndex == adIndex + 1 || it.ad != 2
            }.let { if (it == -1) eventDataList.size else startIndex + it }

        return eventDataList.subList(startIndex, endIndex)
    }

    fun getSamplesBeforeFirstSsaiAd(eventDataList: MutableList<EventData>): MutableList<EventData> {
        val endIndex = eventDataList.indexOfFirst { it.ad == 2 }.let { if (it == -1) eventDataList.size else it }

        return eventDataList.subList(0, endIndex)
    }

    fun getSamplesBetweenAds(
        eventDataList: MutableList<EventData>,
        firstAdIndex: Int,
    ): MutableList<EventData> {
        val firstAdIndex = eventDataList.indexOfFirst { it.adIndex == firstAdIndex }
        if (firstAdIndex == -1) return mutableListOf()

        val firstNonAdIndexRelative = eventDataList.subList(firstAdIndex, eventDataList.size).indexOfFirst { it.ad != 2 }
        if (firstNonAdIndexRelative == -1) return mutableListOf()

        val startIndex = firstAdIndex + firstNonAdIndexRelative
        var endIndex = eventDataList.indexOfFirst { it.adIndex == firstAdIndex + 1 }.let { if (it == -1) eventDataList.size else it }

        return eventDataList.subList(startIndex, endIndex)
    }

    fun getAllSamplesAfterSsaiAdWithIndex(
        eventDataList: MutableList<EventData>,
        adIndex: Int,
    ): MutableList<EventData> {
        val adStartIndex = eventDataList.indexOfFirst { it.adIndex == adIndex }
        if (adStartIndex == -1) return mutableListOf()

        val firstNonAdIndexRelative = eventDataList.subList(adStartIndex, eventDataList.size).indexOfFirst { it.ad != 2 }
        if (firstNonAdIndexRelative == -1) return mutableListOf()

        val startIndex = adStartIndex + firstNonAdIndexRelative

        return eventDataList.subList(startIndex, eventDataList.size)
    }

    fun verifyDataForSsaiAdSamples(
        eventDataList: MutableList<EventData>,
        adBreakMetadata: SsaiAdBreakMetadata,
        adMetadata: SsaiAdMetadata,
        expectedCustomData: CustomData,
        adIndex: Int,
    ) {
        for ((index, eventData) in eventDataList.withIndex()) {
            assertThat(eventData.ad).isEqualTo(2)
            if (index == 0) {
                assertThat(eventData.adIndex).isEqualTo(adIndex)
            } else {
                assertThat(eventData.adIndex).isNull()
            }
            assertThat(eventData.adPosition).isEqualTo(adBreakMetadata.adPosition.toString())
            assertThat(eventData.adSystem).isEqualTo(adMetadata.adSystem)
            assertThat(eventData.adId).isEqualTo(adMetadata.adId)
            verifyCustomData(eventData, expectedCustomData)
        }
    }

    fun verifyHasNoSsaiAdSamples(eventDataList: List<EventData>) {
        val errorSamples =
            eventDataList.filter { x ->
                x.ad == 2 ||
                    x.adIndex != null ||
                    x.adId != null ||
                    x.adSystem != null ||
                    x.adPosition != null
            }

        assertThat(errorSamples.size).isEqualTo(0)
    }

    private fun verifyOnlyOneSampleHasState(
        eventDataList: MutableList<EventData>,
        state: String,
    ) {
        val samplesWithState = eventDataList.filter { x -> x.state?.lowercase() == state }
        assertThat(samplesWithState.size).isEqualTo(1)
    }

    private fun verifyAtLeastOneSampleHasState(
        eventDataList: MutableList<EventData>,
        state: String,
    ) {
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

        // if playingStartEndDelta is very low we just skip verification to avoid flakyness
        if (playingStartEndDelta < 100) {
            return
        }

        // we use a range of -25% to +10% to account for some inaccuracies in the players
        assertThat(playingStartEndDelta).isBetween((playingDuration * 0.75).toLong(), (playingDuration * 1.10).toLong())
    }
}
