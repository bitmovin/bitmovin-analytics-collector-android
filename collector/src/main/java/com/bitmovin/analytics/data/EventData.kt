package com.bitmovin.analytics.data

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.BuildConfig
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.utils.Util

class EventData(
    bitmovinAnalyticsConfig: BitmovinAnalyticsConfig,
    sourceMetadata: SourceMetadata?,
    deviceInfo: DeviceInformation,
    val impressionId: String,
    val userId: String
) {
    val userAgent: String = deviceInfo.userAgent
    val deviceInformation: DeviceInformationDto = DeviceInformationDto(deviceInfo.manufacturer, deviceInfo.model, deviceInfo.isTV)
    val language: String = deviceInfo.locale
    val analyticsVersion: String = BuildConfig.VERSION_NAME
    val playerTech: String = Util.PLAYER_TECH

    val key: String? = bitmovinAnalyticsConfig.key
    val playerKey: String? = bitmovinAnalyticsConfig.playerKey
    val videoId: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.videoId else sourceMetadata.videoId
    val videoTitle: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.title else sourceMetadata.title
    val customUserId: String? = bitmovinAnalyticsConfig.customUserId
    val customData1: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.customData1 else sourceMetadata.customData1
    val customData2: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.customData2 else sourceMetadata.customData2
    val customData3: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.customData3 else sourceMetadata.customData3
    val customData4: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.customData4 else sourceMetadata.customData4
    val customData5: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.customData5 else sourceMetadata.customData5
    val customData6: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.customData6 else sourceMetadata.customData6
    val customData7: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.customData7 else sourceMetadata.customData7
    val path: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.path else sourceMetadata.path
    val experimentName: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.experimentName else sourceMetadata.experimentName
    val cdnProvider: String? = if(sourceMetadata == null) bitmovinAnalyticsConfig.cdnProvider else sourceMetadata.cdnProvider
    var player: String? = bitmovinAnalyticsConfig.playerType?.toString()

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
    var retryCount: Int = 0
}
