package com.bitmovin.analytics.media3.listeners

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.media3.Media3PlayerExceptionMapper
import com.bitmovin.analytics.media3.player.Media3ExoPlayerContext
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

internal class PlayerEventListener(private val stateMachine: PlayerStateMachine, private val exoPlayerContext: Media3ExoPlayerContext) : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {
        try {
            Log.d(TAG, "onPlayerError")

            val videoTime = exoPlayerContext.position
            error.printStackTrace()
            val errorCode = Media3PlayerExceptionMapper.map(error)
            if (!stateMachine.isStartupFinished) {
                stateMachine.videoStartFailedReason = VideoStartFailedReason.PLAYER_ERROR
            }
            stateMachine.error(videoTime, errorCode)
        } catch (e: Exception) {
            Log.d(TAG, e.message, e)
        }
    }

    companion object {
        private const val TAG = "PlayerEventListener"
    }
}
