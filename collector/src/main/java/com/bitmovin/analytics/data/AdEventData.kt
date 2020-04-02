package com.bitmovin.analytics.data

import com.bitmovin.analytics.ads.AdBreak
import com.bitmovin.analytics.utils.Util

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
    var time: Long? = null,
        // TODO var timeHovered: Long? = null,
    var timePlayed: Long? = null,
        // TODO var timeUntilHover: Long? = null,
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
    var videoImpressionId: String? = null,
    var userAgent: String? = null,
    var language: String? = null,
    var cdnProvider: String? = null,
    var customData1: String? = null,
    var customData2: String? = null,
    var customData3: String? = null,
    var customData4: String? = null,
    var customData5: String? = null,
    var customData6: String? = null,
    var customData7: String? = null,
    var customUserId: String? = null,
    var domain: String? = null,
    var experimentName: String? = null,
    var key: String? = null,
    var path: String? = null,
    var player: String? = null,
    var playerKey: String? = null,
    var playerTech: String? = null,
    var screenHeight: Int? = null,
    var screenWidth: Int? = null,
    var version: String? = null,
        // TODO var size: String? = null,
    var userId: String? = null,
    var videoId: String? = null,
    var videoTitle: String? = null,
    var videoWindowHeight: Int? = null,
    var videoWindowWidth: Int? = null,
    var playerStartupTime: Long? = null,
    var analyticsVersion: String? = null,
        // TODO var pageLoadTime: Long? = null,
        // TODO var pageLoadType: Long? = null,
    var autoplay: Boolean? = null,
    var platform: String? = null,
    var audioCodec: String? = null,
    var videoCodec: String? = null
) {

    fun setEventData(eventData: EventData) {
        this.videoImpressionId = eventData.impressionId
        this.userAgent = eventData.userAgent
        this.language = eventData.language
        this.cdnProvider = eventData.cdnProvider
        this.customData1 = eventData.customData1
        this.customData2 = eventData.customData2
        this.customData3 = eventData.customData3
        this.customData4 = eventData.customData4
        this.customData5 = eventData.customData5
        this.customData6 = eventData.customData6
        this.customData7 = eventData.customData7
        this.customUserId = eventData.customUserId
        this.domain = eventData.domain
        this.experimentName = eventData.experimentName
        this.key = eventData.key
        this.path = eventData.path
        this.player = eventData.player
        this.playerKey = eventData.playerKey
        this.playerTech = eventData.playerTech
        this.screenHeight = eventData.screenHeight
        this.screenWidth = eventData.screenWidth
        this.version = eventData.version
        // TODO missing
        // this.size = eventData.size
        this.userId = eventData.userId
        this.videoId = eventData.videoId
        this.videoTitle = eventData.videoTitle
        this.videoWindowHeight = eventData.videoWindowHeight
        this.videoWindowWidth = eventData.videoWindowWidth
        this.platform = eventData.platform
        this.audioCodec = eventData.audioCodec
        this.videoCodec = eventData.videoCodec
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

        // TODO timeUntilHover = adSample.
        // TODO timeHovered = adSample.
    }
}
