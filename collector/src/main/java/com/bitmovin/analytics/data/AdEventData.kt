package com.bitmovin.analytics.data

data class AdEventData(
        var wrapperAdsCount: Long? = null,
        var adSkippable: Boolean? = null,
        var adSkippableAfter: Long? = null,
        var adClickthroughUrl: String? = null,
        var adDescription: String? = null,
        var adDuration: Long? = null,
        var adId: String? = null,
        var adImpressionId: String? = null,
        var adPlaybackHeight: Long? = null,
        var adPlaybackWidth: Long? = null,
        var adStartupTime: Long? = null,
        var adSystem: String? = null,
        var adTitle: String? = null,
        var advertiserName: String? = null,
        var apiFramework: String? = null,
        var audioBitrate: Long? = null,
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
        // TODO var percentageInViewport: Long? = null,
        var quartile1: Long? = 0,
        var quartile3: Long? = 0,
        var skipped: Long? = 0,
        var skipPosition: Long? = null,
        var started: Long? = 0,
        var streamFormat: String? = null,
        var surveyUrl: String? = null,
        var time: Long? = null,
        // TODO var timeHovered: Long? = null,
        // TODO var timeInViewport: Long? = null,
        var timePlayed: Long? = null,
        // TODO var timeUntilHover: Long? = null,
        var universalAdIdRegistry: String? = null,
        var universalAdIdValue: String? = null,
        var videoBitrate: Long? = null,
        var adPodPosition: Long? = null,
        var exitPosition: Long? = null,
        var playPercentage: Long? = null,
        var skipPercentage: Long? = null,
        var clickPercentage: Long? = null,
        var closePercentage: Long? = null,
        var errorPosition: Long? = null,
        var errorPercentage: Long? = null,
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
        var errorCode: Long? = null,
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
        // TODO var autoplay: Boolean? = null,
        var platform: String? = null,
        var audioCodec: String? = null,
        var videoCodec: String? = null) {

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

    fun setAdBreakSample(adBreakSample: AdBreakSample) {
        adPosition = adBreakSample.adPosition
        adOffset = adBreakSample.adOffset
        adScheduleTime = adBreakSample.adScheduleTime
        adReplaceContentDuration = adBreakSample.adReplaceContentDuration
        adPreloadOffset = adBreakSample.adPreloadOffset
        adTagPath = adBreakSample.adTagPath
        adTagServer = adBreakSample.adTagServer
        adTagType = adBreakSample.adTagType
        adTagUrl = adBreakSample.adTagUrl
        adIsPersistent = adBreakSample.adIsPersistent
        adIdPlayer = adBreakSample.adIdPlayer
        manifestDownloadTime = adBreakSample.manifestDownloadTime
        errorCode = adBreakSample.errorCode
        errorData  = adBreakSample.errorData
        errorMessage = adBreakSample.errorMessage
        adFallbackIndex = adBreakSample.adFallbackIndex
    }

    fun setAdSample(adSample: AdSample?) {
        if(adSample == null) {
            return
        }

        wrapperAdsCount = adSample.wrapperAdsCount
        adSkippable = adSample.adSkippable
        adSkippableAfter = adSample.adSkippableAfter
        adClickthroughUrl = adSample.adClickthroughUrl
        adDescription = adSample.adDescription
        adDuration = adSample.adDuration
        adId = adSample.adId
        adPlaybackHeight = adSample.adPlaybackHeight
        adPlaybackWidth = adSample.adPlaybackWidth
        adStartupTime = adSample.adStartupTime
        adSystem = adSample.adSystem
        adTitle = adSample.adTitle
        advertiserName = adSample.advertiserName
        apiFramework = adSample.apiFramework
        audioBitrate = adSample.audioBitrate
        clicked = adSample.clicked
        clickPosition = adSample.clickPosition
        closed = adSample.closed
        closePosition = adSample.closePosition
        completed = adSample.completed
        creativeAdId = adSample.creativeAdId
        creativeId = adSample.creativeId
        dealId = adSample.dealId
        isLinear = adSample.isLinear
        mediaPath = adSample.mediaPath
        mediaServer = adSample.mediaServer
        mediaUrl = adSample.mediaUrl
        midpoint = adSample.midpoint
        minSuggestedDuration = adSample.minSuggestedDuration
        quartile1 = adSample.quartile1
        quartile3 = adSample.quartile3
        skipped = adSample.skipped
        skipPosition = adSample.skipPosition
        started = adSample.started
        streamFormat = adSample.streamFormat
        surveyUrl = adSample.surveyUrl
        // TODO timeHovered = adSample.
        // TODO timeInViewport = adSample.
        timePlayed = adSample.timePlayed
        // TODO timeUntilHover = adSample.
        universalAdIdRegistry = adSample.universalAdIdRegistry
        universalAdIdValue = adSample.universalAdIdValue
        videoBitrate = adSample.videoBitrate
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
    }
}