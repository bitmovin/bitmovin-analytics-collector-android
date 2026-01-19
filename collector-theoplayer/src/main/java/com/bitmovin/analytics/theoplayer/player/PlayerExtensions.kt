package com.bitmovin.analytics.theoplayer.player

import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.player.ReadyState
import com.theoplayer.android.api.player.track.mediatrack.MediaTrack
import com.theoplayer.android.api.player.track.mediatrack.quality.AudioQuality
import com.theoplayer.android.api.player.track.mediatrack.quality.VideoQuality
import com.theoplayer.android.api.player.track.texttrack.TextTrack
import com.theoplayer.android.api.player.track.texttrack.TextTrackMode
import com.theoplayer.android.api.source.TypedSource
import java.lang.Double.isFinite
import kotlin.Boolean
import kotlin.Long

// This is similar how it is tracked in the conviva adapter
internal fun Player.isPlaying(): Boolean {
    if (this.isPaused && this.isEnded) {
        return false
    }

    return this.readyState >= ReadyState.HAVE_FUTURE_DATA
}

internal fun Player.currentPositionInMs(): Long {
    return Util.secondsToMillis(this.currentTime)
}

internal fun Player.isLiveStream(): Boolean? {
    return if (!this.duration.isNaN()) {
        !isFinite(this.duration)
    } else {
        null
    }
}

internal fun Player.getActiveSource(): TypedSource? {
    return this.source?.sources?.firstOrNull()
}

internal fun Player.getDrmType(): String? {
    val drmConfig = this.getActiveSource()?.drm

    if (drmConfig == null) {
        return null
    }

    return PlayerUtils.getDrmTypeFromConfiguration(drmConfig)
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
    val duration = this.duration
    if (duration.isFinite()) {
        return Util.secondsToMillis(duration)
    }

    return 0
}

internal fun Player.getCurrentActiveTextTrack(): TextTrack? {
    val enabledTextTrack =
        this.textTracks.firstOrNull {
            it.mode == TextTrackMode.SHOWING
        }
    if (enabledTextTrack == null) {
        return null
    }

    return enabledTextTrack
}
