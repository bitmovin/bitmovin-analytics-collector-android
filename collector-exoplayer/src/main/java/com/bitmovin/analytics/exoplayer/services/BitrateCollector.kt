package com.bitmovin.analytics.exoplayer.services

import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO
import com.google.android.exoplayer2.C.TRACK_TYPE_VIDEO
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format

class BitrateCollector(private val exoplayer: ExoPlayer) : EventDataManipulator {
    private var currentAudioFormat: Format? = null
    private var currentVideoFormat: Format? = null

    fun hasAudioBitrateChanged(newFormat: Format): Boolean {
        val oldFormat = currentAudioFormat
        return oldFormat == null || newFormat.bitrate.toLong() != oldFormat.bitrate.toLong()
    }

    fun hasVideoBitrateChanged(newFormat: Format): Boolean {
        val oldFormat = currentVideoFormat
        return oldFormat == null || newFormat.bitrate.toLong() != oldFormat.bitrate.toLong()
    }

    fun setNewAudioBitrate(format: Format) {
        this.currentAudioFormat = format
    }

    fun setNewVideoBitrate(format: Format) {
        this.currentVideoFormat = format
    }

    override fun manipulate(data: EventData) {
        getFormatsFromPlayerIfNull()
        applyVideoFormat(data, currentVideoFormat)
        applyAudioFormat(data, currentAudioFormat)
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
    }

    private fun applyAudioFormat(data: EventData, audioFormat: Format?) {
        if (audioFormat == null) {
            return
        }

        data.audioBitrate = audioFormat.bitrate
    }

    private fun getFormatsFromPlayerIfNull() {
        if (currentVideoFormat == null) {
            currentVideoFormat = getCurrentFormatFromPlayer(TRACK_TYPE_VIDEO)
        }
        if (currentAudioFormat == null) {
            currentAudioFormat = getCurrentFormatFromPlayer(TRACK_TYPE_AUDIO)
        }
    }

    private fun getCurrentFormatFromPlayer(trackType: Int): Format? {
        if (exoplayer.currentTrackSelections == null) {
            return null
        }

        val trackSelection = exoplayer.currentTrackSelections.all
                .filterIndexed { i, _ -> exoplayer.getRendererType(i) == trackType }
                .firstOrNull() ?: return null

        var format = trackSelection.getFormat(0) ?: null
        try {
            val getSelectedTracksMethod = trackSelection.javaClass.getMethod("getSelectedFormat")
            format = getSelectedTracksMethod.invoke(trackSelection) as Format
        } catch (e: Exception) {
        }

        return format
    }
}
