package com.bitmovin.analytics.data

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.BuildConfig
import com.bitmovin.analytics.utils.Util

class EventData(
    bitmovinAnalyticsConfig: BitmovinAnalyticsConfig,
    deviceInfo: DeviceInformation,
    val impressionId: String,
    val userId: String
) {
    val userAgent: String = deviceInfo.userAgent
    val deviceInformation: DeviceInformationDto = DeviceInformationDto(deviceInfo.manufacturer, deviceInfo.model, deviceInfo.isTV)
    val language: String = deviceInfo.locale
    val analyticsVersion: String = BuildConfig.VERSION_NAME
    val playerTech: String = Util.PLAYER_TECH

    val key: String? = bitmovinAnalyticsConfig.getKey()
    val playerKey: String? = bitmovinAnalyticsConfig.getPlayerKey()
    val videoId: String? = bitmovinAnalyticsConfig.getVideoId()
    val videoTitle: String? = bitmovinAnalyticsConfig.getTitle()
    val customUserId: String? = bitmovinAnalyticsConfig.getCustomUserId()
    val customData1: String? = bitmovinAnalyticsConfig.getCustomData1()
    val customData2: String? = bitmovinAnalyticsConfig.getCustomData2()
    val customData3: String? = bitmovinAnalyticsConfig.getCustomData3()
    val customData4: String? = bitmovinAnalyticsConfig.getCustomData4()
    val customData5: String? = bitmovinAnalyticsConfig.getCustomData5()
    val customData6: String? = bitmovinAnalyticsConfig.getCustomData6()
    val customData7: String? = bitmovinAnalyticsConfig.customData7
    val path: String? = bitmovinAnalyticsConfig.getPath()
    val experimentName: String? = bitmovinAnalyticsConfig.experimentName
    val cdnProvider: String? = bitmovinAnalyticsConfig.getCdnProvider()
    var player: String? = bitmovinAnalyticsConfig.getPlayerType()?.toString()

    val domain: String = deviceInfo.packageName
    val screenHeight: Int = deviceInfo.screenHeight
    val screenWidth: Int = deviceInfo.screenWidth
    var isLive: Boolean = false
    var isCasting: Boolean = false
    var videoDuration: Long = 0
    var time: Long = Util.getTimestamp()
    var videoWindowWidth: Int = 0
    var videoWindowHeight: Int = 0
    var droppedFrames: Int = 0
    var played: Long = 0
    var buffered: Long = 0
    var paused: Long = 0
    var ad: Int = 0
    var seeked: Long = 0
    var videoPlaybackWidth: Int = 0
    var videoPlaybackHeight: Int = 0
    var videoBitrate: Int = 0
    var audioBitrate: Int = 0
    var videoTimeStart: Long = 0
    var videoTimeEnd: Long = 0
    var videoStartupTime: Long = 0
    var duration: Long = 0
    var startupTime: Long = 0
    var state: String? = null
    var errorCode: Int? = null
    var errorMessage: String? = null
    var errorData: String? = null
    var playerStartupTime: Int = 0
    var pageLoadType: Int = 1
    var pageLoadTime: Int = 0
    var version: String? = null
    var streamFormat: String? = null
    var mpdUrl: String? = null
    var m3u8Url: String? = null
    var progUrl: String? = null
    var isMuted = false
    var sequenceNumber: Int = 0
    val platform: String = if (deviceInfo.isTV) "androidTV" else "android"
    var videoCodec: String? = null
    var audioCodec: String? = null
    var supportedVideoCodecs: List<String>? = null
    var subtitleEnabled: Boolean = false
    var subtitleLanguage: String? = null
    var audioLanguage: String? = null
    var drmType: String? = null
    var drmLoadTime: Long? = null
    var videoStartFailed: Boolean = false
    var videoStartFailedReason: String? = null
    var downloadSpeedInfo: DownloadSpeedInfo? = null
    var retry: Int = 0
}
