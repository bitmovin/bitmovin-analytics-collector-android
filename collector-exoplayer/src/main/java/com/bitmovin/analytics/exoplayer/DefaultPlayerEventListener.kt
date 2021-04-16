package com.bitmovin.analytics.exoplayer

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

abstract class DefaultPlayerEventListener: Player.EventListener {
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}

    override fun onSeekProcessed() {}

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

    override fun onIsLoadingChanged(isLoading: Boolean) {}

    override fun onPlayerError(error: ExoPlaybackException) {}

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {}

    override fun onLoadingChanged(isLoading: Boolean) {}

    override fun onPositionDiscontinuity(reason: Int) {}

    override fun onRepeatModeChanged(repeatMode: Int) {}

    override fun onPlaybackStateChanged(state: Int) {}

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {}

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {}

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {}

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}

    override fun onIsPlayingChanged(isPlaying: Boolean) {}

    override fun onExperimentalOffloadSchedulingEnabledChanged(offloadSchedulingEnabled: Boolean) {}

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {}
}
