package com.bitmovin.analytics.exoplayer

import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player

/**
 * ExoPlayer already has default implementations for all methods of the interface,
 * but on some devices the code crashes with a `AbstractMethodException`, so we need
 * our own default implementation as well.
 */
@Deprecated("Remove default listeners and implement interface Player.Listener directly")
abstract class DefaultPlayerEventListener : Player.Listener {
    override fun onPlayerError(error: PlaybackException) {}
}
