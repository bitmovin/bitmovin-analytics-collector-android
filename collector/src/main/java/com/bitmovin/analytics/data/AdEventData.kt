package com.bitmovin.analytics.data

data class AdEventData(
        var xwrapperAdsCount: Long? = null,
        var xadSkippable: Boolean? = null,
        var xadSkippableAfter: Long? = null,
        var xadClickthroughUrl: String? = null,
        var xadDescription: String? = null,
        var xadDuration: Long? = null,
        var xadId: String? = null,
        var adImpressionId: String? = null,
        var xadPlaybackHeight: Long? = null,
        var xadPlaybackWidth: Long? = null,
        var xadStartupTime: Long? = null,
        var xadSystem: String? = null,
        var xadTitle: String? = null,
        var xadvertiserName: String? = null,
        var xapiFramework: String? = null,
        var xaudioBitrate: Long? = null,
        var xclicked: Long? = 0,
        var xclickPosition: Long? = null,
        var xclosed: Long? = 0,
        var xclosePosition: Long? = null,
        var xcompleted: Long? = 0,
        var xcreativeAdId: String? = null,
        var xcreativeId: String? = null,
        var xdealId: String? = null,
        var xisLinear: Boolean? = null,
        var xmediaPath: String? = null,
        var xmediaServer: String? = null,
        var xmediaUrl: String? = null,
        var xmidpoint: Long? = 0,
        var xminSuggestedDuration: Long? = null,
        // TODO var percentageInViewport: Long? = null,
        var xquartile1: Long? = 0,
        var xquartile3: Long? = 0,
        var xskipped: Long? = 0,
        var xskipPosition: Long? = null,
        var xstarted: Long? = 0,
        var xstreamFormat: String? = null,
        var xsurveyUrl: String? = null,
        var time: Long? = null,
        var xtimeHovered: Long? = null,
        var xtimeInViewport: Long? = null,
        var xtimePlayed: Long? = null,
        var xtimeUntilHover: Long? = null,
        var xuniversalAdIdRegistry: String? = null,
        var xuniversalAdIdValue: String? = null,
        var xvideoBitrate: Long? = null,
        var xadPodPosition: Long? = null,
        var xexitPosition: Long? = null,
        var xplayPercentage: Long? = null,
        var xskipPercentage: Long? = null,
        var xclickPercentage: Long? = null,
        var xclosePercentage: Long? = null,
        var xerrorPosition: Long? = null,
        var xerrorPercentage: Long? = null,
        var xtimeToContent: Long? = null,
        var xtimeFromContent: Long? = null,
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

    fun setAdBreakData(adBreakData: AdBreakData) {
        adPosition = adBreakData.adPosition
        adOffset = adBreakData.adOffset
        adScheduleTime = adBreakData.adScheduleTime
        adReplaceContentDuration = adBreakData.adReplaceContentDuration
        adPreloadOffset = adBreakData.adPreloadOffset
        adTagPath = adBreakData.adTagPath
        adTagServer = adBreakData.adTagServer
        adTagType = adBreakData.adTagType
        adTagUrl = adBreakData.adTagUrl
        adIsPersistent = adBreakData.adIsPersistent
        adIdPlayer = adBreakData.adIdPlayer
        manifestDownloadTime = adBreakData.manifestDownloadTime
        errorCode = adBreakData.errorCode
        errorData  = adBreakData.errorData
        errorMessage = adBreakData.errorMessage
        adFallbackIndex = adBreakData.adFallbackIndex
    }

    fun setAdData(adData: AdData?) {
        if(adData == null) {
            return
        }
    }
}