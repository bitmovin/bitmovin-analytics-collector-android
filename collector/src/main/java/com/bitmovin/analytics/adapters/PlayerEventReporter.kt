package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.SubtitleDto

/**
 * Player-agnostic playback event API for the core collector module.
 *
 * Player-specific collectors translate their native player events into these
 * semantic calls instead of driving the [com.bitmovin.analytics.stateMachines.PlayerStateMachine]
 * directly. The implementation ([DefaultPlayerEventReporter]) owns all analytics
 * state-transition decisions, so adapters do not need to know about
 * `PlayerStateMachine`, the `PlayerStates` enum or `SampleTriggerReason`.
 *
 * All positions are in milliseconds of media time.
 */
interface PlayerEventReporter {
    /**
     * The player intends to start or resume playback (e.g. native "play" event).
     * The implementation decides whether this initiates startup or is a no-op
     * (startup is only initiated while it has not finished yet).
     */
    fun onPlay(position: Long)

    /**
     * The player has actually started rendering frames (e.g. native "playing" event).
     */
    fun onPlaying(position: Long)

    /**
     * Playback was paused. The implementation maps this to the correct state
     * depending on whether startup has finished.
     */
    fun onPause(position: Long)

    /**
     * A seek has started.
     *
     * @param positionBeforeSeek media position *before* the seek (the position the
     *   user is seeking away from).
     */
    fun onSeekStarted(positionBeforeSeek: Long)

    /**
     * The player started stalling / rebuffering during playback. The implementation
     * suppresses this while seeking and before startup has finished.
     */
    fun onBuffering(position: Long)

    fun onBufferingEnded(playerActivity: PlayerActivity)

    /**
     * Periodic player time update. Drives the cached video time and heartbeat logic.
     * The current position is read from the [PlayerContext], so no position parameter
     * is required.
     */
    fun onTimeUpdate()

    /**
     * The source changed within an ongoing session (e.g. a playlist transition),
     * keeping the same collector attached.
     *
     * @param oldPosition end position of the previous source.
     * @param newPosition start position of the new source.
     * @param willAutoplay whether playback continues automatically on the new source,
     *   in which case startup of the new session is initiated immediately.
     */
    fun onSourceChange(
        oldPosition: Long,
        newPosition: Long,
        willAutoplay: Boolean,
    )

    /**
     * The current source was unloaded and the session must be closed and reset
     * (last sample of the session is sent and the state machine is reset back to
     * its initial state).
     */
    fun onSourceUnloaded()

    /**
     * A live program change happened (e.g. for live streams where the source stays
     * the same but the program metadata changes).
     *
     * @param onAfterSessionReset invoked after the previous session has been closed
     *   so the adapter can apply the new source metadata before the new session
     *   sample is emitted.
     */
    fun onProgramChange(onAfterSessionReset: () -> Unit)

    fun onAudioTrackChanged(
        position: Long,
        oldLanguage: String?,
        newLanguage: String?,
    )

    fun onSubtitleChanged(
        position: Long,
        old: SubtitleDto?,
        new: SubtitleDto?,
    )

    /**
     * The video quality changed.
     *
     * @param changed whether the quality actually changed compared to the current one.
     * @param applyQuality applies the new quality to the adapter's quality provider.
     *   It is always invoked, regardless of whether a state transition happened.
     */
    fun onVideoQualityChanged(
        position: Long,
        changed: Boolean,
        applyQuality: () -> Unit,
    )

    /**
     * The audio quality changed. See [onVideoQualityChanged] for the semantics of
     * [changed] and [applyQuality].
     */
    fun onAudioQualityChanged(
        position: Long,
        changed: Boolean,
        applyQuality: () -> Unit,
    )

    /**
     * A playback error occurred. If the error happens before startup has finished and
     * a real video start was attempted, the implementation flags the session as
     * video-start-failed (player error) before reporting.
     *
     * @param nativeError the original player error, forwarded for error transformation.
     */
    fun onError(
        position: Long,
        error: ErrorCode,
        nativeError: Any?,
    )

    /**
     * Playback reached the end of the content. The session is closed out (paused) and
     * the state machine is reset so a subsequent replay starts a fresh session.
     */
    fun onPlaybackFinished(position: Long)

    /** An ad (break) started playing. */
    fun onAdStarted(position: Long)

    /** An ad (break) finished playing. */
    fun onAdFinished()

    /**
     * The collector is being detached / playback stopped. Sends the last sample of
     * the current session if the current state warrants it.
     */
    fun onStop()

    fun onPlayerDestroy(position: Long)

    fun onPlayerRelease()
}
