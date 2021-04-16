package com.bitmovin.analytics.exoplayer

import android.view.Surface
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import java.io.IOException
import java.lang.Exception

/**
 * ExoPlayer already has default implementations for all methods of the interface,
 * but on some devices the code crashes with a `AbstractMethodException`, so we need
 * our own default implementation as well.
 */
abstract class DefaultAnalyticsListener : AnalyticsListener {
    override fun onSeekProcessed(eventTime: AnalyticsListener.EventTime) {}

    override fun onIsLoadingChanged(eventTime: AnalyticsListener.EventTime, isLoading: Boolean) {}

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: ExoPlaybackException) {}

    override fun onSeekStarted(eventTime: AnalyticsListener.EventTime) {}

    override fun onLoadingChanged(eventTime: AnalyticsListener.EventTime, isLoading: Boolean) {}

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {}

    override fun onDrmKeysLoaded(eventTime: AnalyticsListener.EventTime) {}

    override fun onPlaybackSuppressionReasonChanged(eventTime: AnalyticsListener.EventTime, playbackSuppressionReason: Int) {}

    override fun onSkipSilenceEnabledChanged(eventTime: AnalyticsListener.EventTime, skipSilenceEnabled: Boolean) {}

    override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {}

    override fun onVolumeChanged(eventTime: AnalyticsListener.EventTime, volume: Float) {}

    override fun onDrmKeysRestored(eventTime: AnalyticsListener.EventTime) {}

    override fun onDecoderDisabled(eventTime: AnalyticsListener.EventTime, trackType: Int, decoderCounters: DecoderCounters) {}

    override fun onShuffleModeChanged(eventTime: AnalyticsListener.EventTime, shuffleModeEnabled: Boolean) {}

    override fun onDecoderInputFormatChanged(eventTime: AnalyticsListener.EventTime, trackType: Int, format: Format) {}

    override fun onAudioSessionId(eventTime: AnalyticsListener.EventTime, audioSessionId: Int) {}

    override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format) {}

    override fun onSurfaceSizeChanged(eventTime: AnalyticsListener.EventTime, width: Int, height: Int) {}

    override fun onAudioPositionAdvancing(eventTime: AnalyticsListener.EventTime, playoutStartSystemTimeMs: Long) {}

    override fun onTracksChanged(eventTime: AnalyticsListener.EventTime, trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {}

    override fun onUpstreamDiscarded(eventTime: AnalyticsListener.EventTime, mediaLoadData: MediaLoadData) {}

    override fun onAudioDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializationDurationMs: Long) {}

    override fun onLoadCanceled(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {}

    override fun onDecoderInitialized(eventTime: AnalyticsListener.EventTime, trackType: Int, decoderName: String, initializationDurationMs: Long) {}

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {}

    override fun onDecoderEnabled(eventTime: AnalyticsListener.EventTime, trackType: Int, decoderCounters: DecoderCounters) {}

    override fun onAudioUnderrun(eventTime: AnalyticsListener.EventTime, bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {}

    override fun onMediaItemTransition(eventTime: AnalyticsListener.EventTime, mediaItem: MediaItem?, reason: Int) {}

    override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {}

    override fun onDrmKeysRemoved(eventTime: AnalyticsListener.EventTime) {}

    override fun onMetadata(eventTime: AnalyticsListener.EventTime, metadata: Metadata) {}

    override fun onPlaybackParametersChanged(eventTime: AnalyticsListener.EventTime, playbackParameters: PlaybackParameters) {}

    override fun onDownstreamFormatChanged(eventTime: AnalyticsListener.EventTime, mediaLoadData: MediaLoadData) {}

    override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializationDurationMs: Long) {}

    override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, surface: Surface?) {}

    override fun onBandwidthEstimate(eventTime: AnalyticsListener.EventTime, totalLoadTimeMs: Int, totalBytesLoaded: Long, bitrateEstimate: Long) {}

    override fun onPlayerStateChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, playbackState: Int) {}

    override fun onAudioAttributesChanged(eventTime: AnalyticsListener.EventTime, audioAttributes: AudioAttributes) {}

    override fun onVideoEnabled(eventTime: AnalyticsListener.EventTime, counters: DecoderCounters) {}

    override fun onDrmSessionAcquired(eventTime: AnalyticsListener.EventTime) {}

    override fun onVideoDisabled(eventTime: AnalyticsListener.EventTime, counters: DecoderCounters) {}

    override fun onAudioDisabled(eventTime: AnalyticsListener.EventTime, counters: DecoderCounters) {}

    override fun onDrmSessionManagerError(eventTime: AnalyticsListener.EventTime, error: Exception) {}

    override fun onLoadStarted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {}

    override fun onPlayWhenReadyChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, reason: Int) {}

    override fun onPositionDiscontinuity(eventTime: AnalyticsListener.EventTime, reason: Int) {}

    override fun onRepeatModeChanged(eventTime: AnalyticsListener.EventTime, repeatMode: Int) {}

    override fun onDrmSessionReleased(eventTime: AnalyticsListener.EventTime) {}

    override fun onTimelineChanged(eventTime: AnalyticsListener.EventTime, reason: Int) {}

    override fun onVideoFrameProcessingOffset(eventTime: AnalyticsListener.EventTime, totalProcessingOffsetUs: Long, frameCount: Int) {}

    override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {}

    override fun onAudioEnabled(eventTime: AnalyticsListener.EventTime, counters: DecoderCounters) {}

    override fun onAudioInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format) {}

    override fun onLoadError(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, error: IOException, wasCanceled: Boolean) {}
}
