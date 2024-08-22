package com.bitmovin.analytics.data

import androidx.annotation.Keep
import com.bitmovin.analytics.ads.AdBreak
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.utils.Util

@Keep // Protect from obfuscation in case customers are using proguard
data class AdEventData(
    var wrapperAdsCount: Int? = null,
    var adSkippable: Boolean? = null,
    var adSkippableAfter: Long? = null,
    var adClickthroughUrl: String? = null,
    var adDescription: String? = null,
    var adDuration: Long? = null,
    var adId: String? = null,
    var adImpressionId: String? = null,
    var adPlaybackHeight: Int? = null,
    var adPlaybackWidth: Int? = null,
    var adStartupTime: Long? = null,
    var adSystem: String? = null,
    var adTitle: String? = null,
    var advertiserName: String? = null,
    var apiFramework: String? = null,
    var clicked: Long? = 0,
    var clickPosition: Long? = null,
    var closed: Long? = 0,
    var closePosition: Long? = null,
    var completed: Long? = 0,
    var creativeAdId: String? = null,
    var creativeId: String? = null,
    var dealId: String? = null,
    var isLinear: Boolean? = null,
    var mediaPath: String? = null,
    var mediaServer: String? = null,
    var mediaUrl: String? = null,
    var midpoint: Long? = 0,
    var minSuggestedDuration: Long? = null,
    var quartile1: Long? = 0,
    var quartile3: Long? = 0,
    var skipped: Long? = 0,
    var skipPosition: Long? = null,
    var started: Long? = 0,
    var streamFormat: String? = null,
    var surveyUrl: String? = null,
    val time: Long = Util.timestamp,
    var timePlayed: Long? = null,
    var universalAdIdRegistry: String? = null,
    var universalAdIdValue: String? = null,
    var videoBitrate: Int? = null,
    var adPodPosition: Int? = null,
    var exitPosition: Long? = null,
    var playPercentage: Int? = null,
    var skipPercentage: Int? = null,
    var clickPercentage: Int? = null,
    var closePercentage: Int? = null,
    var errorPosition: Long? = null,
    var errorPercentage: Int? = null,
    var timeToContent: Long? = null,
    var timeFromContent: Long? = null,
    var adPosition: String? = null,
    var adOffset: String? = null,
    var adScheduleTime: Long? = null,
    var adReplaceContentDuration: Long? = null,
    var adPreloadOffset: Long? = null,
    var adTagPath: String? = null,
    var adTagServer: String? = null,
    var adTagType: String? = null,
    var adTagUrl: String? = null,
    var adIsPersistent: Boolean? = null,
    var adIdPlayer: String? = null,
    var manifestDownloadTime: Long? = null,
    var errorCode: Int? = null,
    var errorData: String? = null,
    var errorMessage: String? = null,
    var adFallbackIndex: Long = 0,
    var adModule: String? = null,
    var adModuleVersion: String? = null,
    var videoImpressionId: String,
    var userAgent: String,
    var language: String,
    var cdnProvider: String? = null,
    var customData1: String? = null,
    var customData2: String? = null,
    var customData3: String? = null,
    var customData4: String? = null,
    var customData5: String? = null,
    var customData6: String? = null,
    var customData7: String? = null,
    var customData8: String? = null,
    var customData9: String? = null,
    var customData10: String? = null,
    var customData11: String? = null,
    var customData12: String? = null,
    var customData13: String? = null,
    var customData14: String? = null,
    var customData15: String? = null,
    var customData16: String? = null,
    var customData17: String? = null,
    var customData18: String? = null,
    var customData19: String? = null,
    var customData20: String? = null,
    var customData21: String? = null,
    var customData22: String? = null,
    var customData23: String? = null,
    var customData24: String? = null,
    var customData25: String? = null,
    var customData26: String? = null,
    var customData27: String? = null,
    var customData28: String? = null,
    var customData29: String? = null,
    var customData30: String? = null,
    var customUserId: String? = null,
    var domain: String,
    var experimentName: String? = null,
    var key: String? = null,
    var path: String? = null,
    var player: String,
    var playerKey: String? = null,
    var playerTech: String,
    var screenHeight: Int,
    var screenWidth: Int,
    var version: String? = null,
    var userId: String,
    var videoId: String? = null,
    var videoTitle: String? = null,
    var videoWindowHeight: Int,
    var videoWindowWidth: Int,
    var playerStartupTime: Long? = null,
    var analyticsVersion: String? = null,
    var autoplay: Boolean? = null,
    var platform: String,
    var audioCodec: String? = null,
    var videoCodec: String? = null,
    var retryCount: Int = 0,
    var adIndex: Int? = null,
    var adType: Int,
    var quartile1FailedBeaconUrl: String? = null,
    var midpointFailedBeaconUrl: String? = null,
    var quartile3FailedBeaconUrl: String? = null,
    var completedFailedBeaconUrl: String? = null,
) {
    companion object {
        fun fromEventData(
            eventData: EventData,
            adType: AdType,
        ): AdEventData =
            AdEventData(
                analyticsVersion = eventData.analyticsVersion,
                videoImpressionId = eventData.impressionId,
                userAgent = eventData.userAgent,
                language = eventData.language,
                cdnProvider = eventData.cdnProvider,
                customData1 = eventData.customData1,
                customData2 = eventData.customData2,
                customData3 = eventData.customData3,
                customData4 = eventData.customData4,
                customData5 = eventData.customData5,
                customData6 = eventData.customData6,
                customData7 = eventData.customData7,
                customData8 = eventData.customData8,
                customData9 = eventData.customData9,
                customData10 = eventData.customData10,
                customData11 = eventData.customData11,
                customData12 = eventData.customData12,
                customData13 = eventData.customData13,
                customData14 = eventData.customData14,
                customData15 = eventData.customData15,
                customData16 = eventData.customData16,
                customData17 = eventData.customData17,
                customData18 = eventData.customData18,
                customData19 = eventData.customData19,
                customData20 = eventData.customData20,
                customData21 = eventData.customData21,
                customData22 = eventData.customData22,
                customData23 = eventData.customData23,
                customData24 = eventData.customData24,
                customData25 = eventData.customData25,
                customData26 = eventData.customData26,
                customData27 = eventData.customData27,
                customData28 = eventData.customData28,
                customData29 = eventData.customData29,
                customData30 = eventData.customData30,
                customUserId = eventData.customUserId,
                domain = eventData.domain,
                experimentName = eventData.experimentName,
                key = eventData.key,
                path = eventData.path,
                player = eventData.player,
                playerKey = eventData.playerKey,
                playerTech = eventData.playerTech,
                screenHeight = eventData.screenHeight,
                screenWidth = eventData.screenWidth,
                version = eventData.version,
                userId = eventData.userId,
                videoId = eventData.videoId,
                videoTitle = eventData.videoTitle,
                videoWindowHeight = eventData.videoWindowHeight,
                videoWindowWidth = eventData.videoWindowWidth,
                platform = eventData.platform,
                audioCodec = eventData.audioCodec,
                videoCodec = eventData.videoCodec,
                adType = adType.value,
                streamFormat = eventData.streamFormat,
            )
    }

    fun setCustomData(customData: CustomData) {
        customData1 = customData.customData1
        customData2 = customData.customData2
        customData3 = customData.customData3
        customData4 = customData.customData4
        customData5 = customData.customData5
        customData6 = customData.customData6
        customData7 = customData.customData7
        customData8 = customData.customData8
        customData9 = customData.customData9
        customData10 = customData.customData10
        customData11 = customData.customData11
        customData12 = customData.customData12
        customData13 = customData.customData13
        customData14 = customData.customData14
        customData15 = customData.customData15
        customData16 = customData.customData16
        customData17 = customData.customData17
        customData18 = customData.customData18
        customData19 = customData.customData19
        customData20 = customData.customData20
        customData21 = customData.customData21
        customData22 = customData.customData22
        customData23 = customData.customData23
        customData24 = customData.customData24
        customData25 = customData.customData25
        customData26 = customData.customData26
        customData27 = customData.customData27
        customData28 = customData.customData28
        customData29 = customData.customData29
        customData30 = customData.customData30
        experimentName = customData.experimentName
    }

    fun setAdBreak(adBreak: AdBreak) {
        adPosition = adBreak.position?.toString()
        adOffset = adBreak.offset
        adScheduleTime = adBreak.scheduleTime
        adReplaceContentDuration = adBreak.replaceContentDuration
        adPreloadOffset = adBreak.preloadOffset
        val hostnameAndPath = Util.getHostnameAndPath(adBreak.tagUrl)
        adTagServer = hostnameAndPath.first
        adTagPath = hostnameAndPath.second
        adTagType = adBreak.tagType?.toString()
        adTagUrl = adBreak.tagUrl
        adIsPersistent = adBreak.persistent
        adIdPlayer = adBreak.id
        adFallbackIndex = adBreak.fallbackIndex
    }

    fun setAdSample(adSample: AdSample?) {
        if (adSample == null) {
            return
        }

        wrapperAdsCount = adSample.ad.wrapperAdsCount
        adSkippable = adSample.ad.skippable
        adSkippableAfter = adSample.ad.skippableAfter
        adClickthroughUrl = adSample.ad.clickThroughUrl
        adDescription = adSample.ad.description
        adDuration = adSample.ad.duration
        adId = adSample.ad.id
        adPlaybackHeight = adSample.ad.height
        adPlaybackWidth = adSample.ad.width
        adSystem = adSample.ad.adSystemName
        adTitle = adSample.ad.title
        advertiserName = adSample.ad.advertiserName
        apiFramework = adSample.ad.apiFramework
        creativeAdId = adSample.ad.creativeAdId
        creativeId = adSample.ad.creativeId
        dealId = adSample.ad.dealId
        isLinear = adSample.ad.isLinear
        mediaUrl = adSample.ad.mediaFileUrl
        minSuggestedDuration = adSample.ad.minSuggestedDuration
        streamFormat = adSample.ad.mimeType
        surveyUrl = adSample.ad.surveyUrl
        universalAdIdRegistry = adSample.ad.universalAdIdRegistry
        universalAdIdValue = adSample.ad.universalAdIdValue
        videoBitrate = adSample.ad.bitrate
        val hostnameAndPath = Util.getHostnameAndPath(adSample.ad.mediaFileUrl ?: "")
        mediaServer = hostnameAndPath.first
        mediaPath = hostnameAndPath.second
        adStartupTime = adSample.adStartupTime
        clicked = adSample.clicked
        clickPosition = adSample.clickPosition
        closed = adSample.closed
        closePosition = adSample.closePosition
        completed = adSample.completed
        midpoint = adSample.midpoint
        quartile1 = adSample.quartile1
        quartile3 = adSample.quartile3
        skipped = adSample.skipped
        skipPosition = adSample.skipPosition
        started = adSample.started
        timePlayed = adSample.timePlayed
        adPodPosition = adSample.adPodPosition
        exitPosition = adSample.exitPosition
        playPercentage = adSample.playPercentage
        skipPercentage = adSample.skipPercentage
        clickPercentage = adSample.clickPercentage
        closePercentage = adSample.closePercentage
        errorPosition = adSample.errorPosition
        errorPercentage = adSample.errorPercentage
        timeToContent = adSample.timeToContent
        timeFromContent = adSample.timeFromContent
        errorCode = adSample.errorCode
        errorData = adSample.errorData
        errorMessage = adSample.errorMessage
    }
}
