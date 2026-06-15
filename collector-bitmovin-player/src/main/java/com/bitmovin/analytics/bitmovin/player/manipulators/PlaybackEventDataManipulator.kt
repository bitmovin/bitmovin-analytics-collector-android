package com.bitmovin.analytics.bitmovin.player.manipulators

import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.bitmovin.player.BitmovinSdkAdapter
import com.bitmovin.analytics.bitmovin.player.BitmovinUtil
import com.bitmovin.analytics.bitmovin.player.player.PlaybackQualityProvider
import com.bitmovin.analytics.bitmovin.player.player.PlayerLicenseProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.CastTech
import com.bitmovin.analytics.enums.DRMType
import com.bitmovin.analytics.enums.StreamFormat
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.drm.ClearKeyConfig
import com.bitmovin.player.api.drm.WidevineConfig
import com.bitmovin.player.api.source.SourceType

internal class PlaybackEventDataManipulator(
    private val player: Player,
    private val playerContext: PlayerContext,
    private val adapter: BitmovinSdkAdapter,
    private val playbackQualityProvider: PlaybackQualityProvider,
    private val playerLicenseProvider: PlayerLicenseProvider,
    private val downloadSpeedMeter: DownloadSpeedMeter,
) : EventDataManipulator {
    @Suppress("DEPRECATION") // player.subtitle and player.audio are deprecated in newer Bitmovin Player SDK versions
    override fun manipulate(data: EventData) {
        val source = adapter.currentSource
        val sourceMetadata = adapter.getCurrentSourceMetadata()

        // videoDuration and isLive,
        var playerIsLive = false
        if (source != null) {
            val duration = source.duration
            if (duration != -1.0) {
                // source is loaded and duration is available
                if (duration == Double.POSITIVE_INFINITY) {
                    playerIsLive = true
                } else {
                    playerIsLive = false
                    data.videoDuration = Util.secondsToMillis(duration)
                }
            }
        }

        data.isLive = sourceMetadata.isLive ?: playerIsLive

        // streamFormat, mpdUrl, and m3u8Url
        if (source != null) {
            val sourceConfig = source.config
            when (sourceConfig.type) {
                SourceType.Hls -> {
                    data.m3u8Url = sourceConfig.url
                    data.streamFormat = StreamFormat.HLS.value
                }

                SourceType.Dash -> {
                    data.mpdUrl = sourceConfig.url
                    data.streamFormat = StreamFormat.DASH.value
                }

                SourceType.Progressive -> {
                    data.progUrl = sourceConfig.url
                    data.streamFormat = StreamFormat.PROGRESSIVE.value
                }

                SourceType.Smooth -> data.streamFormat = StreamFormat.SMOOTH.value
            }
            val drmConfig = sourceConfig.drmConfig
            when {
                drmConfig is WidevineConfig -> data.drmType = DRMType.WIDEVINE.value
                drmConfig is ClearKeyConfig -> data.drmType = DRMType.CLEARKEY.value
                drmConfig != null -> {
                    BitmovinLog.d(TAG, "Warning: unknown DRM Type " + drmConfig.javaClass.simpleName)
                }
            }
        }

        data.isMuted = playerContext.isMuted
        data.version = playerContext.playerVersion

        // isCasting
        data.isCasting = player.isCasting
        if (player.isCasting) {
            data.castTech = CastTech.GoogleCast.value
        }

        // DroppedVideoFrames
        data.droppedFrames = adapter.getAndResetDroppedFrames()

        val videoQualityHolder = playbackQualityProvider.getVideoQualityHolder()
        if (videoQualityHolder != null) {
            data.videoBitrate = videoQualityHolder.currentBitrateFromManifest ?: videoQualityHolder.currentVideoQuality?.bitrate ?: 0
            data.videoPlaybackHeight = videoQualityHolder.currentVideoQuality?.height ?: 0
            data.videoPlaybackWidth = videoQualityHolder.currentVideoQuality?.width ?: 0
            data.videoCodec = videoQualityHolder.currentVideoQuality?.codec
        }

        val audioQuality = playbackQualityProvider.currentAudioQuality
        if (audioQuality != null) {
            data.audioBitrate = audioQuality.bitrate
            data.audioCodec = audioQuality.codec
        }

        // Subtitle info
        val subtitle = BitmovinUtil.getSubtitleDto(player.subtitle)
        data.subtitleLanguage = subtitle.subtitleLanguage
        data.subtitleEnabled = subtitle.subtitleEnabled

        // Audio language
        val audioTrack = player.audio
        if (audioTrack?.id != null) {
            data.audioLanguage = audioTrack.language
        }

        data.downloadSpeedInfo = downloadSpeedMeter.getInfoAndReset()
        data.playerKey = playerLicenseProvider.getBitmovinPlayerLicenseKey(player.config)
    }

    companion object {
        private const val TAG = "PlaybackEventDataManipulator"
    }
}
