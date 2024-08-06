package com.bitmovin.analytics.media3.exoplayer.manipulators

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.dash.manifest.DashManifest
import androidx.media3.exoplayer.hls.HlsManifest
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.enums.StreamFormat
import com.bitmovin.analytics.media3.exoplayer.Media3ExoPlayerUtil
import com.bitmovin.analytics.media3.exoplayer.player.DrmInfoProvider
import com.bitmovin.analytics.media3.exoplayer.player.PlaybackInfoProvider
import com.bitmovin.analytics.media3.exoplayer.player.PlayerStatisticsProvider
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import com.bitmovin.analytics.utils.Util

internal class PlaybackEventDataManipulator(
    private val player: Player,
    private val playbackInfoProvider: PlaybackInfoProvider,
    private val metadataProvider: MetadataProvider,
    private val drmInfoProvider: DrmInfoProvider,
    private val playerStatisticsProvider: PlayerStatisticsProvider,
    private val downloadSpeedMeter: DownloadSpeedMeter,
) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        // ad
        if (player.isPlayingAd) {
            data.ad = AdType.CLIENT_SIDE.value
        }

        // isLive
        data.isLive =
            Util.getIsLiveFromConfigOrPlayer(
                playbackInfoProvider.playerIsReady,
                metadataProvider.getSourceMetadata()?.isLive,
                player.isCurrentMediaItemDynamic,
            )

        // we report 0 videoDuration for live streams to be consistent with other players/platforms
        if (data.isLive) {
            data.videoDuration = 0
        } else {
            val duration = player.duration
            if (duration != C.TIME_UNSET) {
                data.videoDuration = duration
            }
        }

        // version
        data.version = PlayerType.MEDIA3_EXOPLAYER.toString() + "-" + Media3ExoPlayerUtil.playerVersion

        // DroppedVideoFrames
        data.droppedFrames = playerStatisticsProvider.getAndResetDroppedFrames()

        // streamFormat, mpdUrl, and m3u8Url
        setSourceData(data)

        data.downloadSpeedInfo = downloadSpeedMeter.getInfoAndReset()

        // DRM Information
        data.drmType = drmInfoProvider.drmType

        data.isMuted = isMuted(player)

        setSubtitleInfo(data)
        setStreamFormatAndUrl(data)
    }

    /**
     * Is responsible for setting the streamFormat, mpdUrl, progUrl, and m3u8Url in the EventData object
     */
    @OptIn(UnstableApi::class)
    private fun setSourceData(data: EventData) {
        // In https://www.example-video.mp4?token=1234, the path is https://www.example-video.mp4
        val sourcePath = player.currentMediaItem?.localConfiguration?.uri?.toString()?.substringBefore("?")
        val manifest = player.currentManifest

        // Best world scenario, we have a manifest and a uri
        if (Media3ExoPlayerUtil.isDashManifestClassLoaded && manifest is DashManifest) {
            data.streamFormat = StreamFormat.DASH.value
            data.mpdUrl = manifest.location?.toString() ?: playbackInfoProvider.manifestUrl
        } else if (Media3ExoPlayerUtil.isHlsManifestClassLoaded && manifest is HlsManifest) {
            val masterPlaylist: HlsMultivariantPlaylist = manifest.multivariantPlaylist
            data.streamFormat = StreamFormat.HLS.value
            data.m3u8Url = masterPlaylist.baseUri
        } else {
            // If we don't have a manifest, we can extract the information from the uri in a best effort
            val fileExt = sourcePath?.substringAfterLast(".")?.lowercase()
            when (fileExt) {
                "m3u8" -> {
                    data.streamFormat = StreamFormat.HLS.value
                    data.m3u8Url = sourcePath
                }

                "mpd" -> {
                    data.streamFormat = StreamFormat.DASH.value
                    data.mpdUrl = sourcePath
                }

                "ism", "isml" -> {
                    data.streamFormat = StreamFormat.SMOOTH.value
                    data.progUrl = sourcePath
                }

                "mp4", "m4a", "m4s", "webm", "mkv", "ts", "mpg", "mpeg", "flv", "ogg", "wav", "mp3", "aac", "flac", "amr" -> {
                    data.streamFormat = StreamFormat.PROGRESSIVE.value
                    data.progUrl = sourcePath
                }

                else -> {
                    /*
                        We don't know the format of the stream, so we don't set the streamFormat
                        We will also arrive there if the sourcePath is null
                     */
                }
            }
        }
    }

    // it is enough to have volume OR deviceVolume set to muted
    // this means as soon as one is to muted we report it as muted
    private fun isMuted(player: Player): Boolean {
        if (player.isCommandAvailable(Player.COMMAND_GET_VOLUME)) {
            if (player.volume <= 0.01f) {
                return true
            }
        }

        if (player.isCommandAvailable(Player.COMMAND_GET_DEVICE_VOLUME)) {
            if (player.isDeviceMuted || player.deviceVolume <= 0.01f) {
                return true
            }
        }

        return false
    }

    private fun setSubtitleInfo(eventData: EventData) {
        val textTrack = Media3ExoPlayerUtil.getActiveSubtitles(player)
        eventData.subtitleEnabled = textTrack != null
        eventData.subtitleLanguage = textTrack?.language
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun setStreamFormatAndUrl(eventData: EventData) {
        val manifest = player.currentManifest

        // we check if the corresponding class is loaded, since
        // media3 exoplayer is modular and the dash or hls modules
        // might not be included in the dependencies
        if (Media3ExoPlayerUtil.isDashManifestClassLoaded && manifest is DashManifest) {
            eventData.streamFormat = StreamFormat.DASH.value
            eventData.mpdUrl = manifest.location?.toString() ?: playbackInfoProvider.manifestUrl
        } else if (Media3ExoPlayerUtil.isHlsManifestClassLoaded && manifest is HlsManifest) {
            val masterPlaylist: HlsMultivariantPlaylist = manifest.multivariantPlaylist
            eventData.streamFormat = StreamFormat.HLS.value
            eventData.m3u8Url = masterPlaylist.baseUri
        }
    }
}
