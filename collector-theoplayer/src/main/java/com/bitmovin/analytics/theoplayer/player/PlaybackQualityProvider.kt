package com.bitmovin.analytics.theoplayer.player

import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.track.mediatrack.quality.AudioQuality
import com.theoplayer.android.api.player.track.mediatrack.quality.VideoQuality

// This class stores the current quality since we need to
// have the old quality stored when a quality change event comes in
internal class PlaybackQualityProvider(private val player: Player) {
    // if player starts up there is no initial quality change event,
    // thus we use the player quality directly and store it
    var currentVideoQuality: VideoQuality? = null
        get() {
            if (field == null) {
                field = this.player.getCurrentActiveVideoQuality()
            }
            return field
        }

    // TODO: verify if we should update this on every read
    var currentAudioQuality: AudioQuality? = null
        get() {
            if (field == null) {
                field = this.player.getCurrentActiveAudioQuality()
            }
            return field
        }

    fun didVideoQualityChange(newVideoQuality: VideoQuality?): Boolean {
        return (newVideoQuality?.bandwidth != currentVideoQuality?.bandwidth) ||
            (newVideoQuality?.codecs != currentVideoQuality?.codecs) ||
            (newVideoQuality?.height != currentVideoQuality?.height) ||
            (newVideoQuality?.width != currentVideoQuality?.width)
    }

    fun didAudioQualityChange(newAudioQuality: AudioQuality?): Boolean {
        return (currentAudioQuality?.bandwidth != newAudioQuality?.bandwidth) ||
            (currentAudioQuality?.codecs != newAudioQuality?.codecs)
    }

    fun resetPlaybackQualities() {
        currentVideoQuality = null
        currentAudioQuality = null
    }
}
