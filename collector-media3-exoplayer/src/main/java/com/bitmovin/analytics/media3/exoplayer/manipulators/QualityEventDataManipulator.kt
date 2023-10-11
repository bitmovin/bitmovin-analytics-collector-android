package com.bitmovin.analytics.media3.exoplayer.manipulators

import androidx.media3.common.C.TRACK_TYPE_AUDIO
import androidx.media3.common.C.TRACK_TYPE_VIDEO
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.media3.exoplayer.Media3ExoPlayerUtil

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
    private fun applyVideoFormat(data: EventData, videoFormat: Format?) {
        if (videoFormat == null) {
            return
        }

        data.videoBitrate = videoFormat.bitrate
        data.videoPlaybackHeight = videoFormat.height
        data.videoPlaybackWidth = videoFormat.width
        data.videoCodec = videoFormat.codecs
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun applyAudioFormat(data: EventData, audioFormat: Format?) {
        if (audioFormat == null) {
            return
        }

        data.audioBitrate = audioFormat.bitrate
        data.audioCodec = audioFormat.codecs
        data.audioLanguage = audioFormat.language
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun setFormatsFromPlayer() {
        // TODO: clarify why we are using the fallback here.
        // is this to make sure we have some format even when player is not playing??
        currentVideoFormat = exoplayer.videoFormat ?: Media3ExoPlayerUtil.getSelectedFormatFromPlayer(exoplayer, TRACK_TYPE_VIDEO)
        currentAudioFormat = exoplayer.audioFormat ?: Media3ExoPlayerUtil.getSelectedFormatFromPlayer(exoplayer, TRACK_TYPE_AUDIO)
    }
}
