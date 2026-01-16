package com.bitmovin.analytics.theoplayer.manipulators

import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.enums.StreamFormat
import com.bitmovin.analytics.theoplayer.player.PlaybackQualityProvider
import com.bitmovin.analytics.theoplayer.player.PlayerStatisticsProvider
import com.bitmovin.analytics.theoplayer.player.getActiveSource
import com.bitmovin.analytics.theoplayer.player.getCurrentActiveAudioTrack
import com.bitmovin.analytics.theoplayer.player.getCurrentActiveTextTrack
import com.bitmovin.analytics.theoplayer.player.getDurationInMs
import com.bitmovin.analytics.theoplayer.player.isLiveStream
import com.theoplayer.android.api.THEOplayerGlobal
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.source.SourceType

internal class PlaybackEventDataManipulator(
    private val player: Player,
    private val playbackQualityProvider: PlaybackQualityProvider,
    private val metadataProvider: MetadataProvider,
    private val playerStatisticsProvider: PlayerStatisticsProvider,
) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        data.isLive = metadataProvider.getSourceMetadata()?.isLive ?: player.isLiveStream() ?: false

        // we report 0 videoDuration for live streams to be consistent with other players/platforms
        if (data.isLive) {
            data.videoDuration = 0
        } else {
            data.videoDuration = player.getDurationInMs()
        }

        // version
        data.version = PlayerType.THEOPLAYER.toString() + "-" + THEOplayerGlobal.getVersion()

        val droppedFramesAbsolute = player.metrics?.droppedVideoFrames?.toInt() ?: 0
        data.droppedFrames = playerStatisticsProvider.droppedFramesDeltaSinceLastSample(droppedFramesAbsolute)
        setStreamFormatAndUrl(data)

        val activeQuality = playbackQualityProvider.currentVideoQuality
        if (activeQuality != null) {
            data.videoBitrate = activeQuality.bandwidth.toInt()
            data.videoCodec = activeQuality.codecs
            data.videoPlaybackHeight = activeQuality.height
            data.videoPlaybackWidth = activeQuality.width
        }

        val activeAudioQuality = playbackQualityProvider.currentAudioQuality
        if (activeAudioQuality != null) {
            data.audioBitrate = activeAudioQuality.bandwidth.toInt()
            data.audioCodec = activeAudioQuality.codecs
        }

        val activeAudioTrack = player.getCurrentActiveAudioTrack()
        if (activeAudioTrack != null) {
            data.audioLanguage = activeAudioTrack.language ?: activeAudioTrack.label
        }

        val activeTextTrack = player.getCurrentActiveTextTrack()
        data.subtitleEnabled = activeTextTrack != null
        if (activeTextTrack != null) {
            data.subtitleLanguage = activeTextTrack.language ?: activeTextTrack.label
        }
//
//        // DRM Information
//        data.drmType = drmInfoProvider.drmType
//
        data.isMuted = isMuted(player)
    }

    // it is enough to have isMuted to true or volume to 0 to report as muted
    private fun isMuted(player: Player): Boolean {
        if (player.isMuted) {
            return true
        }

        if (player.volume <= 0.01f) {
            return true
        }

        return false
    }

    private fun setStreamFormatAndUrl(data: EventData) {
        val activeSource = player.getActiveSource()
        if (activeSource == null) {
            return
        }

        val sourceType = activeSource.type
        val sourceUrl = activeSource.src

        if (sourceType == SourceType.DASH) {
            data.mpdUrl = sourceUrl
            data.streamFormat = StreamFormat.DASH.value
        } else if (sourceType == SourceType.HLS || sourceType == SourceType.HLSX) {
            data.m3u8Url = sourceUrl
            data.streamFormat = StreamFormat.HLS.value
        } else {
            // TODO: how to store other formats?
            // for now we just dump everything into progressive source
            data.progUrl = sourceUrl
            data.streamFormat = StreamFormat.PROGRESSIVE.value
        }
    }
}
