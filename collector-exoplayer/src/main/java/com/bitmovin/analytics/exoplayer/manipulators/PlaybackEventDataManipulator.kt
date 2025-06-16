package com.bitmovin.analytics.exoplayer.manipulators

import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.enums.StreamFormat
import com.bitmovin.analytics.exoplayer.ExoUtil
import com.bitmovin.analytics.exoplayer.player.DrmInfoProvider
import com.bitmovin.analytics.exoplayer.player.PlaybackInfoProvider
import com.bitmovin.analytics.exoplayer.player.PlayerStatisticsProvider
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import com.bitmovin.analytics.utils.Util
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.dash.manifest.DashManifest
import com.google.android.exoplayer2.source.hls.HlsManifest
import com.google.android.exoplayer2.source.hls.playlist.HlsMultivariantPlaylist

internal class PlaybackEventDataManipulator(
    private val exoPlayer: ExoPlayer,
    private val playbackInfoProvider: PlaybackInfoProvider,
    private val metadataProvider: MetadataProvider,
    private val drmInfoProvider: DrmInfoProvider,
    private val playerStatisticsProvider: PlayerStatisticsProvider,
    private val downloadSpeedMeter: DownloadSpeedMeter,
) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        // ad
        if (exoPlayer.isPlayingAd) {
            data.ad = AdType.CLIENT_SIDE.value
        }

        // isLive
        data.isLive = metadataProvider.getSourceMetadata()?.isLive ?: exoPlayer.isCurrentMediaItemDynamic

        // we report 0 videoDuration for live streams to be consistent with other players/platforms
        if (data.isLive) {
            data.videoDuration = 0
        } else {
            val duration = exoPlayer.duration
            if (duration != C.TIME_UNSET) {
                data.videoDuration = duration
            }
        }

        // version
        data.version = PlayerType.EXOPLAYER.toString() + "-" + ExoUtil.playerVersion

        // DroppedVideoFrames
        data.droppedFrames = playerStatisticsProvider.getAndResetDroppedFrames()

        // streamFormat, mpdUrl, and m3u8Url, progUrl
        setStreamFormatAndUrl(data)

        data.downloadSpeedInfo = downloadSpeedMeter.getInfoAndReset()

        // DRM Information
        data.drmType = drmInfoProvider.drmType

        data.isMuted = isMuted(exoPlayer)

        setSubtitleInfo(data)
    }

    /**
     * Is responsible for setting the streamFormat, mpdUrl, progUrl, and m3u8Url in the EventData object
     */
    private fun setStreamFormatAndUrl(data: EventData) {
        val manifest = exoPlayer.currentManifest

        // Best world scenario, we have a manifest and a uri
        if (ExoUtil.isDashManifestClassLoaded && manifest is DashManifest) {
            data.streamFormat = StreamFormat.DASH.value
            data.mpdUrl = manifest.location?.toString() ?: playbackInfoProvider.manifestUrl
        } else if (ExoUtil.isHlsManifestClassLoaded && manifest is HlsManifest) {
            val masterPlaylist: HlsMultivariantPlaylist = manifest.multivariantPlaylist
            data.streamFormat = StreamFormat.HLS.value
            data.m3u8Url = masterPlaylist.baseUri
        } else {
            val uri = exoPlayer.currentMediaItem?.localConfiguration?.uri
            // If we don't have a manifest, we can extract the information from the uri in a best effort
            uri?.let {
                Util.setEventDataFormatTypeAndUrlBasedOnExtension(data, it)
            }
        }
    }

    // it is enough to have volume OR deviceVolume set to muted
    // this means as soon as one is to muted we report it as muted
    private fun isMuted(exoPlayer: ExoPlayer): Boolean {
        if (exoPlayer.isCommandAvailable(Player.COMMAND_GET_VOLUME)) {
            if (exoPlayer.volume <= 0.01f) {
                return true
            }
        }

        if (exoPlayer.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME)) {
            if (exoPlayer.isDeviceMuted || exoPlayer.deviceVolume <= 0.01f) {
                return true
            }
        }

        return false
    }

    private fun setSubtitleInfo(eventData: EventData) {
        val textTrack = ExoUtil.getActiveSubtitles(exoPlayer)
        eventData.subtitleEnabled = textTrack != null
        eventData.subtitleLanguage = textTrack?.language
    }
}
