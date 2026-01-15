package com.bitmovin.analytics.theoplayer.example

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource

/**
 * Example MainActivity for THEOplayer Analytics Collector
 *
 * This demonstrates basic THEOplayer setup with analytics integration.
 */
class MainActivity : ComponentActivity() {
    private lateinit var theoPlayerView: THEOplayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        theoPlayerView = findViewById(R.id.theoplayer)
        theoPlayerView.fullScreenManager.isFullScreenOrientationCoupled = true

        val playerConfig =
            THEOplayerConfig.Builder()
                .license(PLAYER_LICENSE_KEY)
                .build()

        // Step 2: Create THEOplayerView
        theoPlayerView = THEOplayerView(this, playerConfig)

        val theoPlayerAsset = "https://cdn.theoplayer.com/video/dash/big_buck_bunny/BigBuckBunny_10s_simple_2014_05_09.mpd"
        // liveAsset seems to need some different way of initialising (TheoLive sourcetype)

        val typedSource =
            TypedSource
                .Builder(theoPlayerAsset)
                .type(SourceType.DASH)
                .build()

        val sourceDescription =
            SourceDescription
                .Builder(typedSource)
                .build()

        theoPlayerView.player.source = sourceDescription
        theoPlayerView.player.isAutoplay = true
        theoPlayerView.player.play()

        val btnPlayPause = findViewById<Button>(R.id.btn_playpause)
        btnPlayPause.setOnClickListener {
            if (theoPlayerView.player.isPaused) {
                theoPlayerView.player.play()
            } else {
                theoPlayerView.player.pause()
            }
        }

        val txtPlayStatus = findViewById<TextView>(R.id.txt_playstatus)
        val txtTimeUpdate = findViewById<TextView>(R.id.txt_timeupdate)

        theoPlayerView.player.addEventListener(PlayerEventTypes.PLAY) {
            txtPlayStatus.text = "Play"
        }

        theoPlayerView.player.addEventListener(PlayerEventTypes.PLAYING) {
            txtPlayStatus.text = "Playing"
        }

        theoPlayerView.player.addEventListener(PlayerEventTypes.PAUSE) {
            txtPlayStatus.text = "Paused"
        }

        theoPlayerView.player.addEventListener(PlayerEventTypes.TIMEUPDATE) { timeUpdateEvent ->
            txtTimeUpdate.text = timeUpdateEvent.currentTime.toString()
        }

        theoPlayerView.player.addEventListener(PlayerEventTypes.ERROR) { errorEvent ->
            txtPlayStatus.text = errorEvent.errorObject.code.toString() + " " + errorEvent.errorObject.message
        }

        theoPlayerView.player.addEventListener(PlayerEventTypes.WAITING) { waitingEvent ->
            txtPlayStatus.text = "WAITING"
        }
    }

    override fun onResume() {
        super.onResume()
        theoPlayerView.onResume()
    }

    override fun onPause() {
        super.onPause()
        theoPlayerView.onPause()
    }

    override fun onDestroy() {
        theoPlayerView.onDestroy()
        super.onDestroy()
    }

    companion object {
        private const val PLAYER_LICENSE_KEY = ""
    }
}
