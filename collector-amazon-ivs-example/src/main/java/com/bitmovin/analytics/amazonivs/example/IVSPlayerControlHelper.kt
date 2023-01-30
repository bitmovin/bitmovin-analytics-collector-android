package com.bitmovin.analytics.amazonivs.example

import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.constraintlayout.widget.ConstraintLayout
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerControlView
import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.Quality
import kotlin.math.roundToInt

// Helper class to manage the binding of SurfaceView, PlayerControlView and Player instance
class IVSPlayerControlHelper(
    private val playerControlView: PlayerControlView,
    private val surfaceView: SurfaceView,
    private var player: Player,
) : SurfaceHolder.Callback, Player.Listener() {
    private var surface: Surface? = null

    init {
        // to set the players surface for drawing
        surfaceView.holder.addCallback(this)

        // to make the controls appear again
        surfaceView.setOnClickListener {
            playerControlView.showControls(true)
        }
        bindPlayer(player)
    }

    fun bindPlayer(player: Player) {
        player.setSurface(surface)
        player.addListener(this)
        playerControlView.setPlayer(player)
    }

    fun release() {
        player.setSurface(null)
        player.removeListener(this)
        player.release()
    }

    private fun setSurfaceViewSize(width: Int, height: Int) {
        val ratio = height.toFloat() / width.toFloat()
        val parentWidth = (surfaceView.parent as ConstraintLayout).measuredWidth
        val newLayout = ConstraintLayout.LayoutParams(parentWidth, (parentWidth * ratio).roundToInt())
        newLayout.topToBottom = R.id.tableLayout
        surfaceView.layoutParams = newLayout
    }

    // #region SurfaceHolder.Callback methods
    override fun surfaceCreated(holder: SurfaceHolder) {
        surface = holder.surface
        player.setSurface(surface)
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surface = null
        player.setSurface(null)
    }
    // #endregion

    // #region Player.Listener
    override fun onVideoSizeChanged(p0: Int, p1: Int) {
        setSurfaceViewSize(p0, p1)
    }

    override fun onCue(p0: Cue) {
    }

    override fun onDurationChanged(p0: Long) {
    }

    override fun onStateChanged(p0: Player.State) {
    }

    override fun onError(p0: PlayerException) {
    }

    override fun onRebuffering() {
    }

    override fun onSeekCompleted(p0: Long) {
    }

    override fun onQualityChanged(p0: Quality) {
    }
    // #endregion
}
