package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.SubtitleDto
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.stateMachines.SampleTriggerReason

/**
 * Default [PlayerEventReporter] implementation that translates player-agnostic
 * playback events into [PlayerStateMachine] transitions.
 *
 * This is the single owner of the state-transition rules that were previously
 * duplicated (and subtly divergent) across the player-specific adapters, e.g. the
 * "only start up while startup is not finished", "suppress buffering while seeking"
 * and "flag video-start-failed on startup errors" guards.
 *
 * The [playerContext] is used to read live playback state (e.g. whether the player
 * is playing after a seek) so adapters do not have to push that information in.
 */
internal class DefaultPlayerEventReporter(
    private val stateMachine: PlayerStateMachine,
    private val playerContext: PlayerContext,
    private val ssaiService: SsaiService,
    private val bitmovinAnalytics: BitmovinAnalytics,
) : PlayerEventReporter {
    override fun onPlay(position: Long) {
        // Startup is only initiated once per session; subsequent play events are no-ops here.
        if (!stateMachine.isStartupFinished) {
            stateMachine.transitionState(PlayerStates.STARTUP, position)
        }
    }

    override fun onPlaying(position: Long) {
        stateMachine.transitionState(PlayerStates.PLAYING, position)
    }

    override fun onPause(position: Long) {
        stateMachine.pause(position)
    }

    override fun onSeekStarted(positionBeforeSeek: Long) {
        stateMachine.seekStarted(positionBeforeSeek)
    }

    override fun onBuffering(position: Long) {
        // Some players emit buffering while seeking; the seek state owns that period.
        if (stateMachine.currentState === PlayerStates.SEEKING) {
            return
        }
        // Buffering before startup has finished is owned by the startup tracking.
        if (!stateMachine.isStartupFinished) {
            return
        }
        stateMachine.transitionState(PlayerStates.BUFFERING, position)
    }

    override fun onBufferingEnded(playerActivity: PlayerActivity) {
        // Don't let a stall ending complete startup; that is owned by the Play/Playing flow.
        if (!stateMachine.isStartupFinished) {
            return
        }
        if (playerActivity == PlayerActivity.PLAYING) {
            this.onPlaying(playerContext.position)
        } else if (playerActivity == PlayerActivity.PAUSED) {
            this.onPause(playerContext.position)
        }
    }

    override fun onTimeUpdate() {
        stateMachine.handlePlayerTimeUpdate()
    }

    override fun onSourceChange(
        oldPosition: Long,
        newPosition: Long,
        willAutoplay: Boolean,
    ) {
        ssaiService.flushCurrentActiveAd(true)
        stateMachine.sourceChange(oldPosition, newPosition, willAutoplay)
    }

    override fun onSourceUnloaded() {
        ssaiService.flushCurrentActiveAd(true)
        bitmovinAnalytics.adAnalytics?.flushCurrentActiveAdOnExit()
        stateMachine.triggerLastSampleOfSession(SampleTriggerReason.SOURCE_CHANGE)
        stateMachine.resetStateMachine()
    }

    override fun onProgramChange(onAfterSessionReset: () -> Unit) {
        stateMachine.programChange(onAfterSessionReset)
    }

    override fun onAudioTrackChanged(
        position: Long,
        oldLanguage: String?,
        newLanguage: String?,
    ) {
        stateMachine.audioTrackChanged(position, oldLanguage, newLanguage)
    }

    override fun onSubtitleChanged(
        position: Long,
        old: SubtitleDto?,
        new: SubtitleDto?,
    ) {
        stateMachine.subtitleChanged(position, old, new)
    }

    override fun onVideoQualityChanged(
        position: Long,
        changed: Boolean,
        applyQuality: () -> Unit,
    ) {
        stateMachine.videoQualityChanged(position, changed, applyQuality)
    }

    override fun onAudioQualityChanged(
        position: Long,
        changed: Boolean,
        applyQuality: () -> Unit,
    ) {
        stateMachine.audioQualityChanged(position, changed, applyQuality)
    }

    override fun onError(
        position: Long,
        error: ErrorCode,
        nativeError: Any?,
    ) {
        stateMachine.error(position, error, nativeError)
    }

    override fun onAdStarted(position: Long) {
        stateMachine.startAd(position)
    }

    override fun onAdFinished() {
        stateMachine.endAd()
    }

    override fun onStop() {
        stateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH)
    }

    // TODO: do we need a CSAI flush here?
    override fun onPlayerDestroy(position: Long) {
        ssaiService.flushCurrentActiveAd(true)

        if (stateMachine.isInStartupState()) {
            stateMachine.exitBeforeVideoStart(position)
        } else {
            stateMachine.triggerLastSampleOfSession(SampleTriggerReason.DETACH)
        }
    }

    override fun onPlaybackFinished(position: Long) {
        // Playback reached the end of the content: close out the session as paused and
        // reset the state machine so a subsequent replay starts a fresh session.
        stateMachine.transitionState(PlayerStates.PAUSE, position)
        stateMachine.resetStateMachine()
    }

    override fun onPlayerRelease() {
        stateMachine.resetStateMachine()
    }
}
