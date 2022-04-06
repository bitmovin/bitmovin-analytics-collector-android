package com.bitmovin.analytics.exoplayer

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

/**
 * ExoPlayer already has default implementations for all methods of the interface,
 * but on some devices the code crashes with a `AbstractMethodException`, so we need
 * our own default implementation as well.
 */
abstract class DefaultPlayerEventListener : Player.Listener {
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}

    override fun onSeekProcessed() {}

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

    override fun onIsLoadingChanged(isLoading: Boolean) {}

    override fun onPlayerError(error: PlaybackException) {}

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {}

    override fun onLoadingChanged(isLoading: Boolean) {}

    override fun onPositionDiscontinuity(reason: Int) {}

    override fun onRepeatModeChanged(repeatMode: Int) {}

    override fun onPlaybackStateChanged(state: Int) {}

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}

    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {}

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {}

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}

    override fun onIsPlayingChanged(isPlaying: Boolean) {}

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {}
}
