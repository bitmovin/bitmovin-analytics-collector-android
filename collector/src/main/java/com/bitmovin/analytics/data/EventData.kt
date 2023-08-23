package com.bitmovin.analytics.data

import androidx.annotation.Keep
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.utils.Util

@Keep // Protect from obfuscation in case customers are using proguard
class EventData(
    deviceInfo: DeviceInformation,
    playerInfo: PlayerInfo,
    customData: CustomData,
    val impressionId: String,
    val userId: String,
    val key: String?,
    val videoId: String?,
    val videoTitle: String?,
    val customUserId: String?,
    val path: String?,
    val cdnProvider: String?,
    val userAgent: String,
) {
    val deviceInformation: DeviceInformationDto = DeviceInformationDto(
        deviceInfo.manufacturer,
        deviceInfo.model,
        deviceInfo.isTV,
        deviceInfo.operatingSystem,
        deviceInfo.operatingSystemMajor,
        deviceInfo.operatingSystemMinor,
        deviceInfo.deviceClass,
    )
    val language: String = deviceInfo.locale
    val analyticsVersion: String = Util.analyticsVersion
    val playerTech: String = playerInfo.playerTech
    val domain: String = deviceInfo.domain
    val screenHeight: Int = deviceInfo.screenHeight
    val screenWidth: Int = deviceInfo.screenWidth
    var isLive: Boolean = false
    var isCasting: Boolean = false
    var castTech: String? = null
    var videoDuration: Long = 0
    var time: Long = Util.timestamp
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
    var playerStartupTime: Long = 0
    var pageLoadType: Int = 1
    var pageLoadTime: Int = 0
    var version: String? = null
    var streamFormat: String? = null
    var mpdUrl: String? = null
    var m3u8Url: String? = null
    var progUrl: String? = null
    var isMuted = false
    var sequenceNumber: Int = 0
    val platform: String = Util.getPlatform(deviceInfo.isTV)
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
    val player: String = playerInfo.playerType.toString()
    var playerKey: String? = null

    val customData1 = customData.customData1
    val customData2 = customData.customData2
    val customData3 = customData.customData3
    val customData4 = customData.customData4
    val customData5 = customData.customData5
    val customData6 = customData.customData6
    val customData7 = customData.customData7
    val customData8 = customData.customData8
    val customData9 = customData.customData9
    val customData10 = customData.customData10
    val customData11 = customData.customData11
    val customData12 = customData.customData12
    val customData13 = customData.customData13
    val customData14 = customData.customData14
    val customData15 = customData.customData15
    val customData16 = customData.customData16
    val customData17 = customData.customData17
    val customData18 = customData.customData18
    val customData19 = customData.customData19
    val customData20 = customData.customData20
    val customData21 = customData.customData21
    val customData22 = customData.customData22
    val customData23 = customData.customData23
    val customData24 = customData.customData24
    val customData25 = customData.customData25
    val customData26 = customData.customData26
    val customData27 = customData.customData27
    val customData28 = customData.customData28
    val customData29 = customData.customData29
    val customData30 = customData.customData30
    val experimentName = customData.experimentName

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventData

        if (impressionId != other.impressionId) return false
        if (userId != other.userId) return false
        if (key != other.key) return false
        if (playerKey != other.playerKey) return false
        if (videoId != other.videoId) return false
        if (videoTitle != other.videoTitle) return false
        if (customUserId != other.customUserId) return false
        if (customData1 != other.customData1) return false
        if (customData2 != other.customData2) return false
        if (customData3 != other.customData3) return false
        if (customData4 != other.customData4) return false
        if (customData5 != other.customData5) return false
        if (customData6 != other.customData6) return false
        if (customData7 != other.customData7) return false
        if (customData8 != other.customData8) return false
        if (customData9 != other.customData9) return false
        if (customData10 != other.customData10) return false
        if (customData11 != other.customData11) return false
        if (customData12 != other.customData12) return false
        if (customData13 != other.customData13) return false
        if (customData14 != other.customData14) return false
        if (customData15 != other.customData15) return false
        if (customData16 != other.customData16) return false
        if (customData17 != other.customData17) return false
        if (customData18 != other.customData18) return false
        if (customData19 != other.customData19) return false
        if (customData20 != other.customData20) return false
        if (customData21 != other.customData21) return false
        if (customData22 != other.customData22) return false
        if (customData23 != other.customData23) return false
        if (customData24 != other.customData24) return false
        if (customData25 != other.customData25) return false
        if (customData26 != other.customData26) return false
        if (customData27 != other.customData27) return false
        if (customData28 != other.customData28) return false
        if (customData29 != other.customData29) return false
        if (customData30 != other.customData30) return false
        if (path != other.path) return false
        if (experimentName != other.experimentName) return false
        if (cdnProvider != other.cdnProvider) return false
        if (userAgent != other.userAgent) return false
        if (deviceInformation != other.deviceInformation) return false
        if (language != other.language) return false
        if (analyticsVersion != other.analyticsVersion) return false
        if (playerTech != other.playerTech) return false
        if (domain != other.domain) return false
        if (screenHeight != other.screenHeight) return false
        if (screenWidth != other.screenWidth) return false
        if (isLive != other.isLive) return false
        if (isCasting != other.isCasting) return false
        if (castTech != other.castTech) return false
        if (videoDuration != other.videoDuration) return false
        if (time != other.time) return false
        if (videoWindowWidth != other.videoWindowWidth) return false
        if (videoWindowHeight != other.videoWindowHeight) return false
        if (droppedFrames != other.droppedFrames) return false
        if (played != other.played) return false
        if (buffered != other.buffered) return false
        if (paused != other.paused) return false
        if (ad != other.ad) return false
        if (seeked != other.seeked) return false
        if (videoPlaybackWidth != other.videoPlaybackWidth) return false
        if (videoPlaybackHeight != other.videoPlaybackHeight) return false
        if (videoBitrate != other.videoBitrate) return false
        if (audioBitrate != other.audioBitrate) return false
        if (videoTimeStart != other.videoTimeStart) return false
        if (videoTimeEnd != other.videoTimeEnd) return false
        if (videoStartupTime != other.videoStartupTime) return false
        if (duration != other.duration) return false
        if (startupTime != other.startupTime) return false
        if (state != other.state) return false
        if (errorCode != other.errorCode) return false
        if (errorMessage != other.errorMessage) return false
        if (errorData != other.errorData) return false
        if (playerStartupTime != other.playerStartupTime) return false
        if (pageLoadType != other.pageLoadType) return false
        if (pageLoadTime != other.pageLoadTime) return false
        if (version != other.version) return false
        if (streamFormat != other.streamFormat) return false
        if (mpdUrl != other.mpdUrl) return false
        if (m3u8Url != other.m3u8Url) return false
        if (progUrl != other.progUrl) return false
        if (isMuted != other.isMuted) return false
        if (sequenceNumber != other.sequenceNumber) return false
        if (platform != other.platform) return false
        if (videoCodec != other.videoCodec) return false
        if (audioCodec != other.audioCodec) return false
        if (supportedVideoCodecs != other.supportedVideoCodecs) return false
        if (subtitleEnabled != other.subtitleEnabled) return false
        if (subtitleLanguage != other.subtitleLanguage) return false
        if (audioLanguage != other.audioLanguage) return false
        if (drmType != other.drmType) return false
        if (drmLoadTime != other.drmLoadTime) return false
        if (videoStartFailed != other.videoStartFailed) return false
        if (videoStartFailedReason != other.videoStartFailedReason) return false
        if (downloadSpeedInfo != other.downloadSpeedInfo) return false
        if (retryCount != other.retryCount) return false
        if (player != other.player) return false

        return true
    }

    override fun hashCode(): Int {
        var result = impressionId.hashCode()
        result = 31 * result + userId.hashCode()
        result = 31 * result + (key?.hashCode() ?: 0)
        result = 31 * result + (playerKey?.hashCode() ?: 0)
        result = 31 * result + (videoId?.hashCode() ?: 0)
        result = 31 * result + (videoTitle?.hashCode() ?: 0)
        result = 31 * result + (customUserId?.hashCode() ?: 0)
        result = 31 * result + (customData1?.hashCode() ?: 0)
        result = 31 * result + (customData2?.hashCode() ?: 0)
        result = 31 * result + (customData3?.hashCode() ?: 0)
        result = 31 * result + (customData4?.hashCode() ?: 0)
        result = 31 * result + (customData5?.hashCode() ?: 0)
        result = 31 * result + (customData6?.hashCode() ?: 0)
        result = 31 * result + (customData7?.hashCode() ?: 0)
        result = 31 * result + (customData8?.hashCode() ?: 0)
        result = 31 * result + (customData9?.hashCode() ?: 0)
        result = 31 * result + (customData10?.hashCode() ?: 0)
        result = 31 * result + (customData11?.hashCode() ?: 0)
        result = 31 * result + (customData12?.hashCode() ?: 0)
        result = 31 * result + (customData13?.hashCode() ?: 0)
        result = 31 * result + (customData14?.hashCode() ?: 0)
        result = 31 * result + (customData15?.hashCode() ?: 0)
        result = 31 * result + (customData16?.hashCode() ?: 0)
        result = 31 * result + (customData17?.hashCode() ?: 0)
        result = 31 * result + (customData18?.hashCode() ?: 0)
        result = 31 * result + (customData19?.hashCode() ?: 0)
        result = 31 * result + (customData20?.hashCode() ?: 0)
        result = 31 * result + (customData21?.hashCode() ?: 0)
        result = 31 * result + (customData22?.hashCode() ?: 0)
        result = 31 * result + (customData23?.hashCode() ?: 0)
        result = 31 * result + (customData24?.hashCode() ?: 0)
        result = 31 * result + (customData25?.hashCode() ?: 0)
        result = 31 * result + (customData26?.hashCode() ?: 0)
        result = 31 * result + (customData27?.hashCode() ?: 0)
        result = 31 * result + (customData28?.hashCode() ?: 0)
        result = 31 * result + (customData29?.hashCode() ?: 0)
        result = 31 * result + (customData30?.hashCode() ?: 0)
        result = 31 * result + (path?.hashCode() ?: 0)
        result = 31 * result + (experimentName?.hashCode() ?: 0)
        result = 31 * result + (cdnProvider?.hashCode() ?: 0)
        result = 31 * result + userAgent.hashCode()
        result = 31 * result + deviceInformation.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + analyticsVersion.hashCode()
        result = 31 * result + playerTech.hashCode()
        result = 31 * result + domain.hashCode()
        result = 31 * result + screenHeight
        result = 31 * result + screenWidth
        result = 31 * result + isLive.hashCode()
        result = 31 * result + isCasting.hashCode()
        result = 31 * result + (castTech?.hashCode() ?: 0)
        result = 31 * result + videoDuration.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + videoWindowWidth
        result = 31 * result + videoWindowHeight
        result = 31 * result + droppedFrames
        result = 31 * result + played.hashCode()
        result = 31 * result + buffered.hashCode()
        result = 31 * result + paused.hashCode()
        result = 31 * result + ad
        result = 31 * result + seeked.hashCode()
        result = 31 * result + videoPlaybackWidth
        result = 31 * result + videoPlaybackHeight
        result = 31 * result + videoBitrate
        result = 31 * result + audioBitrate
        result = 31 * result + videoTimeStart.hashCode()
        result = 31 * result + videoTimeEnd.hashCode()
        result = 31 * result + videoStartupTime.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + startupTime.hashCode()
        result = 31 * result + (state?.hashCode() ?: 0)
        result = 31 * result + (errorCode ?: 0)
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (errorData?.hashCode() ?: 0)
        result = 31 * result + playerStartupTime.hashCode()
        result = 31 * result + pageLoadType
        result = 31 * result + pageLoadTime
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + (streamFormat?.hashCode() ?: 0)
        result = 31 * result + (mpdUrl?.hashCode() ?: 0)
        result = 31 * result + (m3u8Url?.hashCode() ?: 0)
        result = 31 * result + (progUrl?.hashCode() ?: 0)
        result = 31 * result + isMuted.hashCode()
        result = 31 * result + sequenceNumber
        result = 31 * result + platform.hashCode()
        result = 31 * result + (videoCodec?.hashCode() ?: 0)
        result = 31 * result + (audioCodec?.hashCode() ?: 0)
        result = 31 * result + (supportedVideoCodecs?.hashCode() ?: 0)
        result = 31 * result + subtitleEnabled.hashCode()
        result = 31 * result + (subtitleLanguage?.hashCode() ?: 0)
        result = 31 * result + (audioLanguage?.hashCode() ?: 0)
        result = 31 * result + (drmType?.hashCode() ?: 0)
        result = 31 * result + (drmLoadTime?.hashCode() ?: 0)
        result = 31 * result + videoStartFailed.hashCode()
        result = 31 * result + (videoStartFailedReason?.hashCode() ?: 0)
        result = 31 * result + (downloadSpeedInfo?.hashCode() ?: 0)
        result = 31 * result + retryCount
        result = 31 * result + player.hashCode()
        return result
    }

    override fun toString(): String {
        return "EventData(impressionId='$impressionId', userId='$userId', key=$key, videoId=$videoId, videoTitle=$videoTitle, customUserId=$customUserId, path=$path, cdnProvider=$cdnProvider, userAgent='$userAgent', deviceInformation=$deviceInformation, language='$language', analyticsVersion='$analyticsVersion', playerTech='$playerTech', domain='$domain', screenHeight=$screenHeight, screenWidth=$screenWidth, isLive=$isLive, isCasting=$isCasting, castTech=$castTech, videoDuration=$videoDuration, time=$time, videoWindowWidth=$videoWindowWidth, videoWindowHeight=$videoWindowHeight, droppedFrames=$droppedFrames, played=$played, buffered=$buffered, paused=$paused, ad=$ad, seeked=$seeked, videoPlaybackWidth=$videoPlaybackWidth, videoPlaybackHeight=$videoPlaybackHeight, videoBitrate=$videoBitrate, audioBitrate=$audioBitrate, videoTimeStart=$videoTimeStart, videoTimeEnd=$videoTimeEnd, videoStartupTime=$videoStartupTime, duration=$duration, startupTime=$startupTime, state=$state, errorCode=$errorCode, errorMessage=$errorMessage, errorData=$errorData, playerStartupTime=$playerStartupTime, pageLoadType=$pageLoadType, pageLoadTime=$pageLoadTime, version=$version, streamFormat=$streamFormat, mpdUrl=$mpdUrl, m3u8Url=$m3u8Url, progUrl=$progUrl, isMuted=$isMuted, sequenceNumber=$sequenceNumber, platform='$platform', videoCodec=$videoCodec, audioCodec=$audioCodec, supportedVideoCodecs=$supportedVideoCodecs, subtitleEnabled=$subtitleEnabled, subtitleLanguage=$subtitleLanguage, audioLanguage=$audioLanguage, drmType=$drmType, drmLoadTime=$drmLoadTime, videoStartFailed=$videoStartFailed, videoStartFailedReason=$videoStartFailedReason, downloadSpeedInfo=$downloadSpeedInfo, retryCount=$retryCount, player='$player', playerKey=$playerKey, customData1=$customData1, customData2=$customData2, customData3=$customData3, customData4=$customData4, customData5=$customData5, customData6=$customData6, customData7=$customData7, customData8=$customData8, customData9=$customData9, customData10=$customData10, customData11=$customData11, customData12=$customData12, customData13=$customData13, customData14=$customData14, customData15=$customData15, customData16=$customData16, customData17=$customData17, customData18=$customData18, customData19=$customData19, customData20=$customData20, customData21=$customData21, customData22=$customData22, customData23=$customData23, customData24=$customData24, customData25=$customData25, customData26=$customData26, customData27=$customData27, customData28=$customData28, customData29=$customData29, customData30=$customData30, experimentName=$experimentName)"
    }
}
