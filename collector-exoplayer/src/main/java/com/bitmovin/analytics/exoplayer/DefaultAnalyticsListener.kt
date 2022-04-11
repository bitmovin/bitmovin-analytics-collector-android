package com.bitmovin.analytics.exoplayer

import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.decoder.DecoderReuseEvaluation
import com.google.android.exoplayer2.source.LoadEventInfo
import com.google.android.exoplayer2.source.MediaLoadData
import java.io.IOException

/**
 * ExoPlayer already has default implementations for all methods of the interface,
 * but on some devices the code crashes with a `AbstractMethodException`, so we need
 * our own default implementation as well.
 */
@Deprecated("Remove default implementation and implement interface AnalyticsListener directly")
abstract class DefaultAnalyticsListener : AnalyticsListener {

    @Deprecated("Use new method")
    override fun onSeekStarted(eventTime: AnalyticsListener.EventTime) {}

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {}

    override fun onDrmKeysLoaded(eventTime: AnalyticsListener.EventTime) {}

    override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {}

    override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {}

    override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {}

    override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {}

    override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime, output: Any, renderTimeMs: Long) {}

    override fun onDrmSessionAcquired(eventTime: AnalyticsListener.EventTime, state: Int) {}

    override fun onPlayWhenReadyChanged(eventTime: AnalyticsListener.EventTime, playWhenReady: Boolean, reason: Int) {}

    override fun onAudioInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {}

    override fun onLoadError(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData, error: IOException, wasCanceled: Boolean) {}
}
