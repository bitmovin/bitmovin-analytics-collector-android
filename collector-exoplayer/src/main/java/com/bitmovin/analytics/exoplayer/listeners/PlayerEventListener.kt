package com.bitmovin.analytics.exoplayer.listeners

import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.exoplayer.ExoPlayerExceptionMapper
import com.bitmovin.analytics.exoplayer.player.ExoPlayerContext
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.BitmovinLog
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player

@Suppress("ktlint:standard:max-line-length")
internal class PlayerEventListener(
    private val stateMachine: PlayerStateMachine,
    private val exoPlayerContext: ExoPlayerContext,
) : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
        try {
            BitmovinLog.d(TAG, "onPlayerError")
            val videoTime = exoPlayerContext.position
            error.printStackTrace()
            val errorCode = ExoPlayerExceptionMapper.map(error)
            if (!stateMachine.isStartupFinished) {
                stateMachine.videoStartFailedReason = VideoStartFailedReason.PLAYER_ERROR
            }
            stateMachine.error(videoTime, errorCode, error)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    companion object {
        private const val TAG = "PlayerEventListener"
    }
}
