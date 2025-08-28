package com.bitmovin.analytics.media3.exoplayer.listeners

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.media3.exoplayer.Media3ExoPlayerExceptionMapper
import com.bitmovin.analytics.media3.exoplayer.player.Media3ExoPlayerContext
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.BitmovinLog

internal class PlayerEventListener(
    private val stateMachine: PlayerStateMachine,
    private val exoPlayerContext: Media3ExoPlayerContext,
) : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
        try {
            BitmovinLog.d(TAG, "onPlayerError")

            val videoTime = exoPlayerContext.position
            error.printStackTrace()
            val errorCode = Media3ExoPlayerExceptionMapper.map(error)
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
