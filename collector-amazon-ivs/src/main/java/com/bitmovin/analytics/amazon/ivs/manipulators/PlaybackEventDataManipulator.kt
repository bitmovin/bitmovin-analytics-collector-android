package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.amazon.ivs.player.AnalyticsEvent
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.StreamFormat

/**
 * Manipulator for playback info
 * Fields handled:
 *  - isMuted
 *  - videoDuration
 *  - videoPlaybackHeight
 *  - videoPlaybackWidth
 *  - m3u8Url
 *  - streamFormat
 *  - isLive
 */
internal class PlaybackEventDataManipulator(
    private val player: Player,
    private val config: BitmovinAnalyticsConfig,
) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        // TODO: data.videoId should it be set by the user or automatically?
        data.isMuted = player.isMuted
        data.videoDuration = player.duration
        setLive(data)
        setUrlInfo(data)
    }

    private var isUrlFetched = false
    private var fetchedUrl: String? = null

    // listener to fetch the url from analyticsEvents
    fun onAnalyticsEvent(name: String, event: AnalyticsEvent) {
        if (isUrlFetched) {
            return
        }
        fetchedUrl = event.url
        isUrlFetched = true
    }

    private fun setUrlInfo(data: EventData) {
        val videoUrl = fetchedUrl ?: return
        val extension: String = videoUrl.substring(videoUrl.lastIndexOf(".")).trim('.')
        when (extension) {
            "m3u8" -> {
                data.m3u8Url = videoUrl
                data.streamFormat = StreamFormat.HLS.value
            }
            "mpd" -> {
                data.mpdUrl = videoUrl
                data.streamFormat = StreamFormat.DASH.value
            }
            "mp4" -> {
                data.progUrl = videoUrl
                data.streamFormat = StreamFormat.PROGRESSIVE.value
            }
        }
    }

    private fun setLive(data: EventData) {
        val configIsLive = config.isLive
        if (configIsLive != null) {
            data.isLive = configIsLive
        } else {
            data.isLive = isPlaybackLive(player)
        }
    }

    private fun isPlaybackLive(player: Player): Boolean {
        return player.duration == -1L
    }
}
