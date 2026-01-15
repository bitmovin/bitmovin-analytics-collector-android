package com.bitmovin.analytics.theoplayer.player

import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.track.mediatrack.MediaTrack
import com.theoplayer.android.api.player.track.mediatrack.quality.AudioQuality
import com.theoplayer.android.api.player.track.mediatrack.quality.VideoQuality
import com.theoplayer.android.api.player.track.texttrack.TextTrack
import com.theoplayer.android.api.player.track.texttrack.TextTrackMode
import java.lang.Double.isFinite
import kotlin.Boolean
import kotlin.Long
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal fun Player.currentPositionInMs(): Long {
    val positionInMs = this.currentTime * 1000
    return positionInMs.toLong()
}

internal fun Player.isLiveStream(): Boolean? {
    return if (!this.duration.isNaN()) {
        !isFinite(this.duration)
    } else {
        null
    }
}

internal fun Player.getCurrentActiveVideoQuality(): VideoQuality? {
    val enabledVideoTrack = this.videoTracks.firstOrNull { it.isEnabled }
    if (enabledVideoTrack == null) {
        return null
    }

    return enabledVideoTrack.activeQuality
}

internal fun Player.getCurrentActiveAudioQuality(): AudioQuality? {
    val enabledAudioTrack = this.audioTracks.firstOrNull { it.isEnabled }
    if (enabledAudioTrack == null) {
        return null
    }

    return enabledAudioTrack.activeQuality
}

internal fun Player.getCurrentActiveAudioTrack(): MediaTrack<AudioQuality>? {
    val enabledAudioTrack = this.audioTracks.firstOrNull { it.isEnabled }
    if (enabledAudioTrack == null) {
        return null
    }

    return enabledAudioTrack
}

internal fun Player.getDurationInMs(): Long {
    return this.duration.toDuration(DurationUnit.SECONDS).toLong(DurationUnit.MILLISECONDS)
}

internal fun Player.getCurrentActiveTextTrack(): TextTrack? {
    // FIXME: should we track hidden?
    val enabledTextTrack =
        this.textTracks.firstOrNull {
            it.mode == TextTrackMode.SHOWING || it.mode == TextTrackMode.HIDDEN
        }
    if (enabledTextTrack == null) {
        return null
    }

    return enabledTextTrack
}
