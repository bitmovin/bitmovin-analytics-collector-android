package com.bitmovin.analytics.data

data class AdEventData(
    var wrapperAdsCount: Long?,
    var adSkippable: Boolean?,
    var adSkippableAfter: Long?,
    var adClickthroughUrl: String?,
    var adDescription: String?,
    var adDuration: Long?,
    var adId: String?,
    var adImpressionId: String?,
    var adPlaybackHeight: Long?,
    var adPlaybackWidth: Long?,
    var adStartupTime: Long?,
    var adSystem: String?,
    var adTitle: String?,
    var advertiserName: String?,
    var apiFramework: String?,
    var audioBitrate: Long?,
    var clicked: Long? = 0,
    var clickPosition: Long?,
    var closed: Long? = 0,
    var closePosition: Long?,
    var completed: Long? = 0,
    var creativeAdId: String?,
    var creativeId: String?,
    var dealId: String?,
    var isLinear: Boolean?,
    var mediaPath: String?,
    var mediaServer: String?,
    var mediaUrl: String?,
    var midpoint: Long? = 0,
    var minSuggestedDuration: Long?,
    var percentageInViewport: Long?,
    var quartile1: Long? = 0,
    var quartile3: Long? = 0,
    var skipped: Long? = 0,
    var skipPosition: Long?,
    var started: Long? = 0,
    var streamFormat: String?,
    var surveyUrl: String?,
    var time: Long?,
    var timeHovered: Long?,
    var timeInViewport: Long?,
    var timePlayed: Long?,
    var timeUntilHover: Long?,
    var universalAdIdRegistry: String?,
    var universalAdIdValue: String?,
    var videoBitrate: Long?,
    var adPodPosition: Long?,
    var exitPosition: Long?,
    var playPercentage: Long?,
    var skipPercentage: Long?,
    var clickPercentage: Long?,
    var closePercentage: Long?,
    var errorPosition: Long?,
    var errorPercentage: Long?,
    var timeToContent: Long?,
    var timeFromContent: Long?,
    var adPosition: String?,
    var adOffset: String?,
    var adScheduleTime: Long?,
    var adReplaceContentDuration: Long?,
    var adPreloadOffset: Long?,
    var adTagPath: String?,
    var adTagServer: String?,
    var adTagType: String?,
    var adTagUrl: String?,
    var adIsPersistent: Boolean?,
    var adIdPlayer: String?,
    var manifestDownloadTime: Long?,
    var errorCode: Long?,
    var errorData: String?,
    var errorMessage: String?,
    var adFallbackIndex: Long = 0,
    var adModule: String?,
    var adModuleVersion: String?,
    var videoImpressionId: String?,
    var userAgent: String?,
    var language: String?,
    var cdnProvider: String?,
    var customData1: String?,
    var customData2: String?,
    var customData3: String?,
    var customData4: String?,
    var customData5: String?,
    var customUserId: String?,
    var domain: String?,
    var experimentName: String?,
    var key: String?,
    var path: String?,
    var player: String?,
    var playerKey: String?,
    var playerTech: String?,
    var screenHeight: Int?,
    var screenWidth: Int?,
    var version: String?,
    var size: String?,
    var userId: String?,
    var videoId: String?,
    var videoTitle: String?,
    var videoWindowHeight: Int?,
    var videoWindowWidth: Int?,
    var playerStartupTime: Long?,
    var analyticsVersion: String?,
    var pageLoadTime: Long?,
    var pageLoadType: Long?,
    var autoplay: Boolean?,
    var platform: String,
    var audioCodec: String?,
    var videoCodec: String?) {

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
        // missing
        // this.size = eventData.size
        this.userId = eventData.userId
        this.videoId = eventData.videoId
        this.videoTitle = eventData.videoTitle
        this.videoWindowHeight = eventData.videoWindowHeight
        this.videoWindowWidth = eventData.videoWindowWidth
        this.audioCodec = eventData.audioCodec
        this.videoCodec = eventData.videoCodec
    }
}