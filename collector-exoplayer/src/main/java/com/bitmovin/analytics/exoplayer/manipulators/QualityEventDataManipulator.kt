package com.bitmovin.analytics.exoplayer.manipulators

import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.exoplayer.ExoUtil
import com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO
import com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format

internal class QualityEventDataManipulator(private val exoplayer: ExoPlayer) : EventDataManipulator {
    var currentAudioFormat: Format? = null
    var currentVideoFormat: Format? = null

    override fun manipulate(data: EventData) {
        applyVideoFormat(data, currentVideoFormat)
        applyAudioFormat(data, currentAudioFormat)
    }

    fun hasAudioFormatChanged(newFormat: Format?): Boolean {
        newFormat ?: return false
        val oldFormat = currentAudioFormat
        return oldFormat == null || newFormat.bitrate.toLong() != oldFormat.bitrate.toLong()
    }

    fun hasVideoFormatChanged(newFormat: Format?): Boolean {
        newFormat ?: return false
        val oldFormat = currentVideoFormat
        return oldFormat == null ||
            newFormat.bitrate.toLong() != oldFormat.bitrate.toLong() ||
            newFormat.width != oldFormat.width ||
            newFormat.height != oldFormat.height
    }

    fun reset() {
        currentAudioFormat = null
        currentVideoFormat = null
    }

    private fun applyVideoFormat(data: EventData, videoFormat: Format?) {
        if (videoFormat == null) {
            return
        }

        data.videoBitrate = videoFormat.bitrate
        data.videoPlaybackHeight = videoFormat.height
        data.videoPlaybackWidth = videoFormat.width
        data.videoCodec = videoFormat.codecs
    }

    private fun applyAudioFormat(data: EventData, audioFormat: Format?) {
        if (audioFormat == null) {
            return
        }

        data.audioBitrate = audioFormat.bitrate
        data.audioCodec = audioFormat.codecs
        data.audioLanguage = audioFormat.language
    }

    fun setFormatsFromPlayer() {
        currentVideoFormat = exoplayer.videoFormat ?: ExoUtil.getSelectedFormatFromPlayer(exoplayer, TRACK_TYPE_VIDEO)
        currentAudioFormat = exoplayer.audioFormat ?: ExoUtil.getSelectedFormatFromPlayer(exoplayer, TRACK_TYPE_AUDIO)
    }
}
