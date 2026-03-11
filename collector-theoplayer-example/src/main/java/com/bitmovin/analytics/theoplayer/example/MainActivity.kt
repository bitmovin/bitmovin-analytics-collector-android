package com.bitmovin.analytics.theoplayer.example

import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.LogLevel
import com.bitmovin.analytics.test.utils.TestSources
import com.bitmovin.analytics.theoplayer.api.ITHEOplayerCollector
import com.theoplayer.android.api.THEOplayerConfig
import com.theoplayer.android.api.THEOplayerView
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.source.SourceDescription
import com.theoplayer.android.api.source.SourceType
import com.theoplayer.android.api.source.TypedSource
import com.theoplayer.android.api.source.addescription.GoogleImaAdDescription

/**
 * Example MainActivity for THEOplayer Analytics Collector
 *
 * This demonstrates basic THEOplayer setup with analytics integration.
 */
@Suppress("ktlint:standard:max-line-length")
class MainActivity : ComponentActivity() {
    private lateinit var theoPlayerView: THEOplayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val playerConfig =
            THEOplayerConfig.Builder()
                .license(TESTING_LICENSE)
                .build()

        theoPlayerView = THEOplayerView(this, playerConfig)
        theoPlayerView.fullScreenManager.isFullScreenOrientationCoupled = true

        val playerContainer = findViewById<FrameLayout>(R.id.playerContainer)
        playerContainer.addView(theoPlayerView)

        val analyticsConfig = AnalyticsConfig(licenseKey = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0", logLevel = LogLevel.DEBUG)
        val collector = ITHEOplayerCollector.create(this, analyticsConfig)
        collector.attachPlayer(theoPlayerView.player)

        val theoPlayerAsset = "https://cdn.theoplayer.com/video/dash/big_buck_bunny/BigBuckBunny_10s_simple_2014_05_09.mpd"

        val typedSource =
            TypedSource
                .Builder(theoPlayerAsset)
                .type(SourceType.DASH)
                .build()

        val preRollAd1 =
            GoogleImaAdDescription
                .Builder(TestSources.IMA_AD_SOURCE_3)
                .timeOffset("start")
                .build()

        val sourceDescription =
            SourceDescription
                .Builder(typedSource)
                .ads(preRollAd1)
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
        private const val TESTING_LICENSE = "sZP7IYe6T6P60l0rIS4eC6zkCl4gFSacIKh-CS3KIOzLISX10ux10SUK3LR6FOPlUY3zWokgbgjNIOf9flxg0LIlIl5zFS5LTDh-3uaZ0Zzr3LaZFSA60SRL3oCZ3Sg1ImfVfK4_bQgZCYxNWoryIQXzImf90SRi0Sat3l5i0u5i0Oi6Io4pIYP1UQgqWgjeCYxgflEc3LC_0L0i0LRo0uCLFOPeWok1dDrLYtA1Ioh6TgV6v6fVfKcqCoXVdQjLUOfVfGxEIDjiWQXrIYfpCoj-fgzVfKxqWDXNWG3ybojkbK3gflNWf6E6FOPVWo31WQ1qbta6FOPzdQ4qbQc1sD4ZFK3qWmPUFOPLIQ-LflNWfKXpIwPqdDa6Ymi6bo4pIXjNWYAZIY3LdDjpflNzbG4gFOPKIDXzUYPgbZf9Dkkj"
    }
}
