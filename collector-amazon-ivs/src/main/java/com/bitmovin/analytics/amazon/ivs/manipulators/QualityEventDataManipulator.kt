package com.bitmovin.analytics.amazon.ivs.manipulators

import android.util.Log
import com.bitmovin.analytics.amazon.ivs.player.PlaybackQualityProvider
import com.bitmovin.analytics.amazon.ivs.player.PlayerStatisticsProvider
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.utils.CodecHelper

/**
 * Manipulator for video quality
 * Fields handled:
 * - droppedFrames
 * - videoBitrate
 * - videoPlaybackWidth
 * - videoPlaybackHeight
 * - videoCodec
 */
internal class QualityEventDataManipulator(private val playbackQualityProvider: PlaybackQualityProvider, private val playerStatisticsProvider: PlayerStatisticsProvider) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        try {
            data.droppedFrames = playerStatisticsProvider.getDroppedFramesDelta()

            // we use the quality data to track stream quality which represents what's coming from the manifest
            // compared to statistics which shows actual played bitrate for example
            val currentQuality = playbackQualityProvider.currentQuality

            if (currentQuality != null) {
                // quality.bitrate is the media bitrate which is including video and audio bitrate
                // the api doesn't support tracking both separately
                data.videoBitrate = currentQuality.bitrate
                data.videoPlaybackWidth = currentQuality.width
                data.videoPlaybackHeight = currentQuality.height

                val codecInfo = extractCodecInfo(currentQuality.codecs)
                data.audioCodec = codecInfo.audioCodec
                data.videoCodec = codecInfo.videoCodec
            }
        } catch (e: Exception) {
            Log.e("QualityDataManipulator", "Something went wrong while setting quality event data, e: ${e.message}", e)
        }
    }

    internal data class CodecInfo(val videoCodec: String?, val audioCodec: String?)

    companion object {
        private fun extractCodecInfo(codecs: String): CodecInfo {
            val splitted = codecs.split(",")

            if (splitted.size != 2) {
                return CodecInfo(null, null)
            }

            var videoCodec: String?
            var audioCodec: String?

            // it is expected but not guaranteed that first item
            // in codecs is video codec and second item is audio codec
            // try to confirm the reverse case when the first item is audio codec and the second item is video codec
            if (CodecHelper.isVideoCodec(splitted[1]) || CodecHelper.isAudioCodec(splitted[0])) {
                videoCodec = splitted[1]
                audioCodec = splitted[0]
            }
            // default
            else {
                videoCodec = splitted[0]
                audioCodec = splitted[1]
            }

            return CodecInfo(videoCodec, audioCodec)
        }
    }
}
