package com.bitmovin.analytics.media3.exoplayer.manipulators

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.dash.manifest.DashManifest
import androidx.media3.exoplayer.hls.HlsManifest
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.enums.StreamFormat
import com.bitmovin.analytics.media3.exoplayer.Media3ExoplayerUtil
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

    private val isDashManifestClassLoaded by lazy {
        Util.isClassLoaded(DASH_MANIFEST_CLASSNAME, this.javaClass.classLoader)
    }
    private val isHlsManifestClassLoaded by lazy {
        Util.isClassLoaded(HLS_MANIFEST_CLASSNAME, this.javaClass.classLoader)
    }

    override fun manipulate(data: EventData) {
        // ad
        if (player.isPlayingAd) {
            data.ad = 1
        }

        // isLive
        data.isLive = Util.getIsLiveFromConfigOrPlayer(
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
        data.version = PlayerType.MEDIA3_EXOPLAYER.toString() + "-" + Media3ExoplayerUtil.playerVersion

        // DroppedVideoFrames
        data.droppedFrames = playerStatisticsProvider.getAndResetDroppedFrames()

        data.downloadSpeedInfo = downloadSpeedMeter.getInfo()

        // DRM Information
        data.drmType = drmInfoProvider.drmType

        data.isMuted = isMuted(player)

        setSubtitleInfo(data)
        setStreamFormatAndUrl(data)
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
        val textTrack = Media3ExoplayerUtil.getSelectedFormatFromPlayer(player, C.TRACK_TYPE_TEXT)
        eventData.subtitleEnabled = textTrack != null
        eventData.subtitleLanguage = textTrack?.language
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun setStreamFormatAndUrl(eventData: EventData) {
        val manifest = player.currentManifest

        // we check if the corresponding class is loaded, since
        // media3 exoplayer is modular and the dash or hls modules
        // might not be included in the dependencies
        if (isDashManifestClassLoaded && manifest is DashManifest) {
            eventData.streamFormat = StreamFormat.DASH.value
            eventData.mpdUrl = manifest.location?.toString() ?: playbackInfoProvider.manifestUrl
        } else if (isHlsManifestClassLoaded && manifest is HlsManifest) {
            val masterPlaylist: HlsMultivariantPlaylist = manifest.multivariantPlaylist
            eventData.streamFormat = StreamFormat.HLS.value
            eventData.m3u8Url = masterPlaylist.baseUri
        }
    }

    companion object {
        private const val DASH_MANIFEST_CLASSNAME =
            "androidx.media3.exoplayer.dash.manifest.DashManifest"
        private const val HLS_MANIFEST_CLASSNAME =
            "androidx.media3.exoplayer.hls.HlsManifest"
    }
}
