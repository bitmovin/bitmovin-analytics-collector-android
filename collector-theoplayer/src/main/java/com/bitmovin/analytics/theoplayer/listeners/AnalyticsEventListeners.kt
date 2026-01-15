package com.bitmovin.analytics.theoplayer.listeners

import android.util.Log
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.theoplayer.errors.TheoPlayerExceptionMapper
import com.bitmovin.analytics.theoplayer.player.PlaybackQualityProvider
import com.bitmovin.analytics.theoplayer.player.convertDoubleSecondsToLongMs
import com.bitmovin.analytics.theoplayer.player.currentPositionInMs
import com.bitmovin.analytics.utils.BitmovinLog
import com.theoplayer.android.api.event.player.DestroyEvent
import com.theoplayer.android.api.event.player.ErrorEvent
import com.theoplayer.android.api.event.player.PauseEvent
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.event.player.PlayingEvent
import com.theoplayer.android.api.event.player.SeekedEvent
import com.theoplayer.android.api.event.player.SeekingEvent
import com.theoplayer.android.api.event.player.SourceChangeEvent
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

    internal fun registerEventListeners() {
        player.addEventListener(PlayerEventTypes.PLAY) { event -> handlePlayEvent(event) }
        player.addEventListener(PlayerEventTypes.PLAYING) { event -> handlePlayingEvent(event) }
        player.addEventListener(PlayerEventTypes.PAUSE) { event -> onPause(event) }
        player.addEventListener(PlayerEventTypes.ENDED) { event -> Log.i(TAG, "Event: ENDED") }
        player.addEventListener(PlayerEventTypes.ERROR) { event -> handleErrorEvent(event) }
        player.addEventListener(PlayerEventTypes.SEEKED) { event -> onSeeked(event) }
        player.addEventListener(PlayerEventTypes.SEEKING) { event -> onSeeking(event) }
        player.addEventListener(PlayerEventTypes.WAITING) { event -> onBuffering(event) }
        player.addEventListener(PlayerEventTypes.SOURCECHANGE) { event -> handleSourceChange(event) }
        player.addEventListener(PlayerEventTypes.RATECHANGE) { event -> Log.i(TAG, "Event: RATECHANGE") }
        player.addEventListener(PlayerEventTypes.VOLUMECHANGE) { event -> Log.i(TAG, "Event: VOLUMECHANGE") }
        player.addEventListener(PlayerEventTypes.PROGRESS) { event -> Log.i(TAG, "Event: PROGRESS") }
        player.addEventListener(PlayerEventTypes.DURATIONCHANGE) { event -> Log.i(TAG, "Event: DURATIONCHANGE") }
        player.addEventListener(PlayerEventTypes.READYSTATECHANGE) { event -> Log.i(TAG, "Event: READYSTATECHANGE") }
        player.addEventListener(PlayerEventTypes.TIMEUPDATE) { event -> Log.i(TAG, "Event: TIMEUPDATE") }
        player.addEventListener(PlayerEventTypes.LOADEDMETADATA) { event -> Log.i(TAG, "Event: LOADEDMETADATA") }
        player.addEventListener(PlayerEventTypes.LOADEDDATA) { event -> Log.i(TAG, "Event: LOADEDDATA") }
        player.addEventListener(PlayerEventTypes.CANPLAY) { event -> Log.i(TAG, "Event: CANPLAY") }
        player.addEventListener(PlayerEventTypes.CANPLAYTHROUGH) { event -> Log.i(TAG, "Event: CANPLAYTHROUGH") }
        player.addEventListener(PlayerEventTypes.SEGMENTNOTFOUND) { event -> Log.i(TAG, "Event: SEGMENTNOTFOUND") }
        player.addEventListener(PlayerEventTypes.ENCRYPTED) { event -> Log.i(TAG, "Event: ENCRYPTED") }
        player.addEventListener(PlayerEventTypes.CONTENTPROTECTIONERROR) { event -> Log.i(TAG, "Event: CONTENTPROTECTIONERROR") }
        player.addEventListener(PlayerEventTypes.CONTENTPROTECTIONSUCCESS) { event -> Log.i(TAG, "Event: CONTENTPROTECTIONSUCCESS") }
        player.addEventListener(
            PlayerEventTypes.NOSUPPORTEDREPRESENTATIONFOUND,
        ) { event -> Log.i(TAG, "Event: NOSUPPORTEDREPRESENTATIONFOUND") }
        player.addEventListener(PlayerEventTypes.PRESENTATIONMODECHANGE) { event -> Log.i(TAG, "Event: PRESENTATIONMODECHANGE") }
        player.addEventListener(PlayerEventTypes.DESTROY) { event -> onDestroy(event) }
        player.addEventListener(PlayerEventTypes.LOADSTART) { event -> Log.i(TAG, "Event: LOADSTART") }
        player.addEventListener(PlayerEventTypes.RESIZE) { event -> Log.i(TAG, "Event: RESIZE") }
    }

    // TODO: removing seems odd this way
    internal fun unregisterEventListeners() {
//        player.removeEventListener(PlayerEventTypes.PLAY) { event -> onStartup(event) }
    }

    private fun handlePlayEvent(playEvent: PlayEvent) {
        Log.i(TAG, "Event: PlayEvent")

        if (!stateMachine.isStartupFinished) {
            startupInitiated(playEvent.currentTime.convertDoubleSecondsToLongMs())
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

    private fun onSeeking(seekEvent: SeekingEvent) {
        Log.i(TAG, "Event: SEEKING")

        // FIXME: this is not correct, given that we would need to position of seekStart and this is
        // returning where we seek to
        val positionAfterSeek = convertDoubleSecondsToMilliseconds(seekEvent.currentTime)
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
            // TODO: handle startup errors
//            if (!stateMachine.isStartupFinished && isVideoAttemptedPlay) {
//                stateMachine.videoStartFailedReason = VideoStartFailedReason.PLAYER_ERROR
//            }
            val errorCode = TheoPlayerExceptionMapper.map(originalNativeError.errorObject)
            stateMachine.error(videoTime, errorCode, originalNativeError)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    companion object {
        private const val TAG = "AnalyticsEventListeners"

        internal fun convertDoubleSecondsToMilliseconds(seconds: Double): Long {
            return (seconds * 1000).toLong()
        }
    }
}
