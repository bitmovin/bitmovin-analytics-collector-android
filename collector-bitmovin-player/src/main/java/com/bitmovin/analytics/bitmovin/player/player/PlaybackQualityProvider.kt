package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.media.audio.quality.AudioQuality
import com.bitmovin.player.api.media.video.quality.VideoQuality
import com.bitmovin.player.api.source.Source

// This class stores the current quality since we need to
// have the old quality stored when a quality change event comes in
internal class PlaybackQualityProvider(private val player: Player) {
    private var videoQualityHolder: VideoQualityHolder? = null

    fun getVideoQualityHolder(): VideoQualityHolder? {
        if (videoQualityHolder == null) {
            videoQualityHolder = this.player.extractVideoQualityInfo()
        }
        return videoQualityHolder
    }

    fun setVideoQuality(videoQuality: VideoQuality?) {
        val videoBitrateFromManifest = this.player.resolveManifestVideoBitrate(videoQuality)
        videoQualityHolder = VideoQualityHolder(videoQuality, videoBitrateFromManifest)
    }

    // Seeds the video quality from a source's manifest when the current quality is not yet known.
    // During a playlist transition the new source's startup sample can be emitted before the player
    // reports a valid VideoPlaybackQualityChanged for it, and [Player.playbackVideoData] still holds
    // the previous source's (stale) quality. Seeding from the new source's manifest gives the startup
    // sample an in-ladder placeholder bitrate that is overwritten by the real quality event moments
    // later, instead of leaking the previous source's bitrate. We pick the lowest rendition since
    // adaptive playback typically starts low and the actual starting quality is not yet known.
    fun seedVideoQualityFromSource(source: Source?) {
        if (videoQualityHolder != null) return
        val seed = source?.availableVideoQualities?.minByOrNull { it.bitrate } ?: return
        videoQualityHolder = VideoQualityHolder(seed, seed.bitrate)
    }

    var currentAudioQuality: AudioQuality? = null
        get() {
            if (field == null) {
                field = this.player.playbackAudioData
            }
            return field
        }

    fun didVideoQualityChange(newVideoQuality: VideoQuality?): Boolean {
        return (newVideoQuality?.bitrate != videoQualityHolder?.currentVideoQuality?.bitrate) ||
            (newVideoQuality?.codec != videoQualityHolder?.currentVideoQuality?.codec) ||
            (newVideoQuality?.height != videoQualityHolder?.currentVideoQuality?.height) ||
            (newVideoQuality?.width != videoQualityHolder?.currentVideoQuality?.width)
    }

    fun didAudioQualityChange(newAudioQuality: AudioQuality?): Boolean {
        return (currentAudioQuality?.bitrate != newAudioQuality?.bitrate) ||
            (currentAudioQuality?.codec != newAudioQuality?.codec)
    }

    fun resetPlaybackQualities() {
        videoQualityHolder = null
        currentAudioQuality = null
    }
}

internal data class VideoQualityHolder(
    val currentVideoQuality: VideoQuality?,
    val currentBitrateFromManifest: Int?,
)
