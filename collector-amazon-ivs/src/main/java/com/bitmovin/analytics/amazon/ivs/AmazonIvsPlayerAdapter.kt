package com.bitmovin.analytics.amazon.ivs

import android.util.Log
import com.amazonaws.ivs.player.Cue
import com.amazonaws.ivs.player.Player
import com.amazonaws.ivs.player.PlayerException
import com.amazonaws.ivs.player.Quality
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.adapters.DefaultPlayerAdapter
import com.bitmovin.analytics.amazon.ivs.playback.VideoStartupService
import com.bitmovin.analytics.amazon.ivs.playback.VodPlaybackService
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import java.nio.ByteBuffer

class AmazonIvsPlayerAdapter(
    private val player: Player,
    config: BitmovinAnalyticsConfig,
    stateMachine: PlayerStateMachine,
    featureFactory: FeatureFactory,
    eventDataFactory: EventDataFactory,
    deviceInformationProvider: DeviceInformationProvider,
    private val videoStartupService: VideoStartupService,
    private val vodPlaybackService: VodPlaybackService,
) : DefaultPlayerAdapter(
    config,
    eventDataFactory,
    stateMachine,
    featureFactory,
    deviceInformationProvider,
),
    EventDataManipulator {

    init {
        attachAnalyticsListener()
        videoStartupService.checkStartup(player.state, player.position)
    }

    private fun attachAnalyticsListener() {
        player.addListener(createAnalyticsListener())
    }

    private fun createAnalyticsListener(): Player.Listener {
        return object : Player.Listener() {

            // not dispatched for live stream
            override fun onAnalyticsEvent(name: String, properties: String) {
                Log.d(TAG, "onAnalyticsEvent name: $name, properties: $properties")
            }

            override fun onMetadata(mediaType: String, data: ByteBuffer) {
                Log.d(TAG, "onMetadata mediaType: $mediaType, data: ${String(data.array())}")
            }

            override fun onCue(p0: Cue) {
//                Log.d(TAG, "onCue $p0")
            }

            override fun onDurationChanged(duration: Long) {
                Log.d(TAG, "onDurationChanged $duration")
            }

            override fun onStateChanged(state: Player.State) {
                Log.d(
                    TAG,
                    "onStateChanged state: $state, position: $position, playerState: ${player.state}, statistics: ${player.statistics}, live: ${player.liveLatency} ${player.isLiveLowLatency} ",
                )
                videoStartupService.onStateChange(state, position)
                vodPlaybackService.onStateChange(state, position)
            }

            override fun onError(p0: PlayerException) {
                Log.d(TAG, "onError")
            }

            override fun onRebuffering() {
                Log.d(TAG, "onRebuffering")
            }

            // This is triggered once the seek completed
            override fun onSeekCompleted(p0: Long) {
                Log.d(TAG, "onSeekCompleted")
            }

            override fun onVideoSizeChanged(p0: Int, p1: Int) {
                Log.d(TAG, "onVideoSizeChanged")
            }

            override fun onQualityChanged(p0: Quality) {
                Log.d(TAG, "onQualityChanged")
            }
        }
    }

    override fun manipulate(data: EventData) {
        data.version = player.version
    }

    override val eventDataManipulators: Collection<EventDataManipulator> by lazy { listOf(this) } // TODO("Not yet implemented")

    override val position: Long
        get() = player.position

    override val drmDownloadTime: Long?
        get() = null // TODO("Not yet implemented")
    override val currentSourceMetadata: SourceMetadata?
        get() = null // TODO("Not yet implemented")

    override fun resetSourceRelatedState() {
//        TODO("Not yet implemented")
    }

    override fun clearValues() {
//        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "AmazonIVSPlayerAdapter"
    }
}
