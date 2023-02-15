package com.bitmovin.analytics.amazon.ivs.manipulators

import android.util.Log
import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.amazon.ivs.player.PlaybackQualityProvider
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator

/**
 * Manipulator for video quality
 * Fields handled:
 * - droppedFrames
 * - videoBitrate
 * - videoPlaybackWidth
 * - videoPlaybackHeight
 * - videoCodec
 */
internal class QualityEventDataManipulator(private val player: Player, private val playbackQualityProvider: PlaybackQualityProvider) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        try {
            data.droppedFrames = getAndSetDroppedFrames(player.statistics.droppedFrames)

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

    private var previousTotalDroppedFrames: Int = 0
    private fun getAndSetDroppedFrames(totalDroppedFrames: Int): Int {
        val currentSampleDroppedFrames = totalDroppedFrames - previousTotalDroppedFrames
        previousTotalDroppedFrames = totalDroppedFrames
        return currentSampleDroppedFrames
    }

    internal data class CodecInfo(val videoCodec: String?, val audioCodec: String?)

    companion object {
        // Extracting the codecs is best effort for now since the order
        // is not guaranteed according to AWS IVS support (video might be first or second parameter)
        // general format is "videoCodec,audioCodec"
        // TODO (AN-3350): improve codec detection
        private fun extractCodecInfo(codecs: String): CodecInfo {
            val splitted = codecs.split(",")

            if (splitted.size != 2) {
                return CodecInfo(null, null)
            }

            val videoCodec = splitted[0]
            val audioCodec = splitted[1]

            return CodecInfo(videoCodec, audioCodec)
        }
    }
}
