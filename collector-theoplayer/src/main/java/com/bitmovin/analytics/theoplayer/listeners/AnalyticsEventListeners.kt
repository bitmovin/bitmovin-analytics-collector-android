package com.bitmovin.analytics.theoplayer.listeners

import android.util.Log
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.theoplayer.errors.TheoPlayerExceptionMapper
import com.bitmovin.analytics.theoplayer.player.PlaybackQualityProvider
import com.bitmovin.analytics.theoplayer.player.currentPositionInMs
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.player.CanPlayEvent
import com.theoplayer.android.api.event.player.CanPlayThroughEvent
import com.theoplayer.android.api.event.player.ContentProtectionErrorEvent
import com.theoplayer.android.api.event.player.ContentProtectionSuccessEvent
import com.theoplayer.android.api.event.player.DestroyEvent
import com.theoplayer.android.api.event.player.DurationChangeEvent
import com.theoplayer.android.api.event.player.EndedEvent
import com.theoplayer.android.api.event.player.ErrorEvent
import com.theoplayer.android.api.event.player.LoadStartEvent
import com.theoplayer.android.api.event.player.LoadedDataEvent
import com.theoplayer.android.api.event.player.LoadedMetadataEvent
import com.theoplayer.android.api.event.player.MediaEncryptedEvent
import com.theoplayer.android.api.event.player.NoSupportedRepresentationFoundEvent
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.event.player.PresentationModeChange
import com.theoplayer.android.api.event.player.ProgressEvent
import com.theoplayer.android.api.event.player.RateChangeEvent
import com.theoplayer.android.api.event.player.ReadyStateChangeEvent
import com.theoplayer.android.api.event.player.ResizeEvent
import com.theoplayer.android.api.event.player.SeekedEvent
import com.theoplayer.android.api.event.player.SeekingEvent
import com.theoplayer.android.api.event.player.SegmentNotFoundEvent
import com.theoplayer.android.api.event.player.SourceChangeEvent
import com.theoplayer.android.api.event.player.TimeUpdateEvent
import com.theoplayer.android.api.event.player.VolumeChangeEvent
import com.theoplayer.android.api.event.player.WaitingEvent
import com.theoplayer.android.api.player.Player

internal class AnalyticsEventListeners(
    private val stateMachine: PlayerStateMachine,
    private val player: Player,
    private val playbackQualityProvider: PlaybackQualityProvider,
) {
    // TODO: these flags are mimicing what bitmovin player is doing
    // we should get rid of these though, and hide this in the statemachine
    // -> effort to make core library more compact
    private var isVideoAttemptedPlay = false

    // Event listener references for registration and unregistration
    private val playListener = EventListener<PlayEvent> { event -> handlePlayEvent(event) }
    private val playingListener = EventListener<PlayingEvent> { event -> handlePlayingEvent(event) }
    private val pauseListener = EventListener<PauseEvent> { event -> onPause(event) }
    private val endedListener = EventListener<EndedEvent> { Log.i(TAG, "Event: ENDED") }
    private val errorListener = EventListener<ErrorEvent> { event -> handleErrorEvent(event) }
    private val seekedListener = EventListener<SeekedEvent> { event -> onSeeked(event) }
    private val seekingListener = EventListener<SeekingEvent> { event -> onSeeking(event) }
    private val waitingListener = EventListener<WaitingEvent> { event -> onBuffering(event) }
    private val sourceChangeListener = EventListener<SourceChangeEvent> { event -> handleSourceChange(event) }
    private val rateChangeListener = EventListener<RateChangeEvent> { Log.i(TAG, "Event: RATECHANGE") }
    private val volumeChangeListener = EventListener<VolumeChangeEvent> { Log.i(TAG, "Event: VOLUMECHANGE") }
    private val progressListener = EventListener<ProgressEvent> { Log.i(TAG, "Event: PROGRESS") }
    private val durationChangeListener = EventListener<DurationChangeEvent> { Log.i(TAG, "Event: DURATIONCHANGE") }
    private val readyStateChangeListener = EventListener<ReadyStateChangeEvent> { Log.i(TAG, "Event: READYSTATECHANGE") }
    private val timeUpdateListener = EventListener<TimeUpdateEvent> { Log.i(TAG, "Event: TIMEUPDATE") }
    private val loadedMetadataListener = EventListener<LoadedMetadataEvent> { Log.i(TAG, "Event: LOADEDMETADATA") }
    private val loadedDataListener = EventListener<LoadedDataEvent> { Log.i(TAG, "Event: LOADEDDATA") }
    private val canPlayListener = EventListener<CanPlayEvent> { Log.i(TAG, "Event: CANPLAY") }
    private val canPlayThroughListener = EventListener<CanPlayThroughEvent> { Log.i(TAG, "Event: CANPLAYTHROUGH") }
    private val segmentNotFoundListener = EventListener<SegmentNotFoundEvent> { Log.i(TAG, "Event: SEGMENTNOTFOUND") }
    private val encryptedListener = EventListener<MediaEncryptedEvent> { Log.i(TAG, "Event: ENCRYPTED") }
    private val contentProtectionErrorListener = EventListener<ContentProtectionErrorEvent> { Log.i(TAG, "Event: CONTENTPROTECTIONERROR") }
    private val contentProtectionSuccessListener =
        EventListener<ContentProtectionSuccessEvent> { Log.i(TAG, "Event: CONTENTPROTECTIONSUCCESS") }
    private val noSupportedRepresentationFoundListener =
        EventListener<NoSupportedRepresentationFoundEvent> { Log.i(TAG, "Event: NOSUPPORTEDREPRESENTATIONFOUND") }
    private val presentationModeChangeListener = EventListener<PresentationModeChange> { Log.i(TAG, "Event: PRESENTATIONMODECHANGE") }
    private val destroyListener = EventListener<DestroyEvent> { event -> onDestroy(event) }
    private val loadStartListener = EventListener<LoadStartEvent> { Log.i(TAG, "Event: LOADSTART") }
    private val resizeListener = EventListener<ResizeEvent> { Log.i(TAG, "Event: RESIZE") }

    internal fun registerEventListeners() {
        player.addEventListener(PlayerEventTypes.PLAY, playListener)
        player.addEventListener(PlayerEventTypes.PLAYING, playingListener)
        player.addEventListener(PlayerEventTypes.PAUSE, pauseListener)
        player.addEventListener(PlayerEventTypes.ENDED, endedListener)
        player.addEventListener(PlayerEventTypes.ERROR, errorListener)
        player.addEventListener(PlayerEventTypes.SEEKED, seekedListener)
        player.addEventListener(PlayerEventTypes.SEEKING, seekingListener)
        player.addEventListener(PlayerEventTypes.WAITING, waitingListener)
        player.addEventListener(PlayerEventTypes.SOURCECHANGE, sourceChangeListener)
        player.addEventListener(PlayerEventTypes.RATECHANGE, rateChangeListener)
        player.addEventListener(PlayerEventTypes.VOLUMECHANGE, volumeChangeListener)
        player.addEventListener(PlayerEventTypes.PROGRESS, progressListener)
        player.addEventListener(PlayerEventTypes.DURATIONCHANGE, durationChangeListener)
        player.addEventListener(PlayerEventTypes.READYSTATECHANGE, readyStateChangeListener)
        player.addEventListener(PlayerEventTypes.TIMEUPDATE, timeUpdateListener)
        player.addEventListener(PlayerEventTypes.LOADEDMETADATA, loadedMetadataListener)
        player.addEventListener(PlayerEventTypes.LOADEDDATA, loadedDataListener)
        player.addEventListener(PlayerEventTypes.CANPLAY, canPlayListener)
        player.addEventListener(PlayerEventTypes.CANPLAYTHROUGH, canPlayThroughListener)
        player.addEventListener(PlayerEventTypes.SEGMENTNOTFOUND, segmentNotFoundListener)
        player.addEventListener(PlayerEventTypes.ENCRYPTED, encryptedListener)
        player.addEventListener(PlayerEventTypes.CONTENTPROTECTIONERROR, contentProtectionErrorListener)
        player.addEventListener(PlayerEventTypes.CONTENTPROTECTIONSUCCESS, contentProtectionSuccessListener)
        player.addEventListener(PlayerEventTypes.NOSUPPORTEDREPRESENTATIONFOUND, noSupportedRepresentationFoundListener)
        player.addEventListener(PlayerEventTypes.PRESENTATIONMODECHANGE, presentationModeChangeListener)
        player.addEventListener(PlayerEventTypes.DESTROY, destroyListener)
        player.addEventListener(PlayerEventTypes.LOADSTART, loadStartListener)
        player.addEventListener(PlayerEventTypes.RESIZE, resizeListener)
    }

    internal fun unregisterEventListeners() {
        player.removeEventListener(PlayerEventTypes.PLAY, playListener)
        player.removeEventListener(PlayerEventTypes.PLAYING, playingListener)
        player.removeEventListener(PlayerEventTypes.PAUSE, pauseListener)
        player.removeEventListener(PlayerEventTypes.ENDED, endedListener)
        player.removeEventListener(PlayerEventTypes.ERROR, errorListener)
        player.removeEventListener(PlayerEventTypes.SEEKED, seekedListener)
        player.removeEventListener(PlayerEventTypes.SEEKING, seekingListener)
        player.removeEventListener(PlayerEventTypes.WAITING, waitingListener)
        player.removeEventListener(PlayerEventTypes.SOURCECHANGE, sourceChangeListener)
        player.removeEventListener(PlayerEventTypes.RATECHANGE, rateChangeListener)
        player.removeEventListener(PlayerEventTypes.VOLUMECHANGE, volumeChangeListener)
        player.removeEventListener(PlayerEventTypes.PROGRESS, progressListener)
        player.removeEventListener(PlayerEventTypes.DURATIONCHANGE, durationChangeListener)
        player.removeEventListener(PlayerEventTypes.READYSTATECHANGE, readyStateChangeListener)
        player.removeEventListener(PlayerEventTypes.TIMEUPDATE, timeUpdateListener)
        player.removeEventListener(PlayerEventTypes.LOADEDMETADATA, loadedMetadataListener)
        player.removeEventListener(PlayerEventTypes.LOADEDDATA, loadedDataListener)
        player.removeEventListener(PlayerEventTypes.CANPLAY, canPlayListener)
        player.removeEventListener(PlayerEventTypes.CANPLAYTHROUGH, canPlayThroughListener)
        player.removeEventListener(PlayerEventTypes.SEGMENTNOTFOUND, segmentNotFoundListener)
        player.removeEventListener(PlayerEventTypes.ENCRYPTED, encryptedListener)
        player.removeEventListener(PlayerEventTypes.CONTENTPROTECTIONERROR, contentProtectionErrorListener)
        player.removeEventListener(PlayerEventTypes.CONTENTPROTECTIONSUCCESS, contentProtectionSuccessListener)
        player.removeEventListener(PlayerEventTypes.NOSUPPORTEDREPRESENTATIONFOUND, noSupportedRepresentationFoundListener)
        player.removeEventListener(PlayerEventTypes.PRESENTATIONMODECHANGE, presentationModeChangeListener)
        player.removeEventListener(PlayerEventTypes.DESTROY, destroyListener)
        player.removeEventListener(PlayerEventTypes.LOADSTART, loadStartListener)
        player.removeEventListener(PlayerEventTypes.RESIZE, resizeListener)
    }

    private fun handlePlayEvent(playEvent: PlayEvent) {
        Log.i(TAG, "Event: PlayEvent")

        if (!stateMachine.isStartupFinished) {
            val currentTimeMs = Util.secondsToMillis(playEvent.currentTime)
            startupInitiated(currentTimeMs)
        }
    }

    private fun handleSourceChange(sourceChangeEvent: SourceChangeEvent) {
        Log.i(TAG, "Event: SourceChange")
        stateMachine.resetStateMachine()
    }

    private fun startupInitiated(currentPositionMs: Long) {
        playbackQualityProvider.resetPlaybackQualities()
        stateMachine.transitionState(PlayerStates.STARTUP, currentPositionMs)

        // TODO: verify if this works
        if (!player.ads.isPlaying) {
            // if ad is playing as first thing we prevent from sending the
            // VideoStartFailedReason.PAGE_CLOSED / VideoStartFailedReason.PLAYER_ERROR
            // because actual video is not playing yet
            isVideoAttemptedPlay = true
        }
    }

    private fun onSeeked(seekedEvent: SeekedEvent) {
        Log.i(TAG, "Event: SEEKED")

        if (player.isPaused) {
            stateMachine.transitionState(PlayerStates.PAUSE, player.currentPositionInMs())
        }
    }

    private fun onSeeking(seekingEvent: SeekingEvent) {
        Log.i(TAG, "Event: SEEKING")
        // FIXME: this is not correct, given that we would need to position of seekStart and this is
        // returning where we seek to
        val positionAfterSeek = Util.secondsToMillis(seekingEvent.currentTime)
        stateMachine.transitionState(PlayerStates.SEEKING, positionAfterSeek)
    }

    private fun handlePlayingEvent(playingEvent: PlayingEvent) {
        Log.i(TAG, "Event: PlayingEvent")
        stateMachine.transitionState(PlayerStates.PLAYING, player.currentPositionInMs())
    }

    private fun onDestroy(destroyEvent: DestroyEvent) {
        Log.i("TAG", "Event DestroyEvent")
        // TODO: we probably need to do a detach here, to also handle ssai and others
        // and clean up
        stateMachine.triggerLastSampleOfSession()
    }

    private fun onPause(pauseEvent: PauseEvent) {
        Log.i(TAG, "Event: PauseEvent")
        stateMachine.pause(player.currentPositionInMs())
    }

    private fun onBuffering(waitingEvent: WaitingEvent) {
        Log.i(TAG, "Event: waitingEvent")
        // optiview player emits waiting event while seeking, thus we don't want to move to buffering
        // TODO: why isn't this handle on state machine level?
        if (stateMachine.currentState == PlayerStates.SEEKING) {
            return
        }
        stateMachine.transitionState(PlayerStates.BUFFERING, player.currentPositionInMs())
    }

    private fun handleErrorEvent(originalNativeError: ErrorEvent) {
        Log.i(TAG, "Event: ErrorEvent")

        try {
            val videoTime = player.currentPositionInMs()
            // TODO: add test for startup error
            if (!stateMachine.isStartupFinished && isVideoAttemptedPlay) {
                stateMachine.videoStartFailedReason = VideoStartFailedReason.PLAYER_ERROR
            }
            val errorCode = TheoPlayerExceptionMapper.map(originalNativeError.errorObject)
            stateMachine.error(videoTime, errorCode, originalNativeError)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    companion object {
        private const val TAG = "AnalyticsEventListeners"
    }
}
