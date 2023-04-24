package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.player.api.Player
import com.bitmovin.player.api.media.audio.quality.AudioQuality
import com.bitmovin.player.api.media.video.quality.VideoQuality

// This class stores the current quality since we need to
// have the old quality stored when a quality change event comes in
internal class PlaybackQualityProvider(private val player: Player) {

    // if player starts up there is no initial quality change event,
    // thus we use the player quality directly and store it
    var currentVideoQuality: VideoQuality? = null
        get() {
            if (field == null) {
                field = this.player.playbackVideoData
            }
            return field
        }

    var currentAudioQuality: AudioQuality? = null
        get() {
            if (field == null) {
                field = this.player.playbackAudioData
            }
            return field
        }

    fun didVideoQualityChange(newVideoQuality: VideoQuality?): Boolean {
        return (newVideoQuality?.bitrate != currentVideoQuality?.bitrate) ||
            (newVideoQuality?.codec != currentVideoQuality?.codec) ||
            (newVideoQuality?.height != currentVideoQuality?.height) ||
            (newVideoQuality?.width != currentVideoQuality?.width)
    }

    fun didAudioQualityChange(newAudioQuality: AudioQuality?): Boolean {
        return (currentAudioQuality?.bitrate != newAudioQuality?.bitrate) ||
            (currentAudioQuality?.codec != newAudioQuality?.codec)
    }

    fun resetPlaybackQualities() {
        currentVideoQuality = null
        currentAudioQuality = null
    }
}
