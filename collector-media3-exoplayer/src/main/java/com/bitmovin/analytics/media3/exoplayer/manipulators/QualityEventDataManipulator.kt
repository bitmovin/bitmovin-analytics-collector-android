package com.bitmovin.analytics.media3.exoplayer.manipulators

import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.dtos.EventData

internal class QualityEventDataManipulator(private val exoplayer: ExoPlayer) : EventDataManipulator {
    var currentAudioFormat: Format? = null
    var currentVideoFormat: Format? = null

    override fun manipulate(data: EventData) {
        applyVideoFormat(data, currentVideoFormat)
        applyAudioFormat(data, currentAudioFormat)
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun hasAudioFormatChanged(newFormat: Format?): Boolean {
        newFormat ?: return false
        val oldFormat = currentAudioFormat
        return oldFormat == null || newFormat.bitrate.toLong() != oldFormat.bitrate.toLong()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
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

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun applyVideoFormat(
        data: EventData,
        videoFormat: Format?,
    ) {
        if (videoFormat == null) {
            return
        }

        data.videoBitrate = videoFormat.bitrate
        data.videoPlaybackHeight = videoFormat.height
        data.videoPlaybackWidth = videoFormat.width
        data.videoCodec = videoFormat.codecs
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun applyAudioFormat(
        data: EventData,
        audioFormat: Format?,
    ) {
        if (audioFormat == null) {
            return
        }

        data.audioBitrate = audioFormat.bitrate
        data.audioCodec = audioFormat.codecs
        data.audioLanguage = audioFormat.language
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun setFormatsFromPlayerOnStartup() {
        currentVideoFormat = exoplayer.videoFormat
        currentAudioFormat = exoplayer.audioFormat
    }
}
