package com.bitmovin.analytics.media3.exoplayer.manipulators

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.dtos.EventData

internal class QualityEventDataManipulator(private val exoplayer: ExoPlayer) : EventDataManipulator {
    var currentAudioFormat: Format? = null

    // Stores the currently playing video format together with its resolved manifest bitrate, so the
    // two values stay in sync and are captured at the same moment. See [VideoFormatHolder].
    var videoFormatHolder: VideoFormatHolder? = null
        private set

    @androidx.annotation.OptIn(UnstableApi::class)
    fun setVideoFormat(videoFormat: Format?) {
        videoFormatHolder = VideoFormatHolder(videoFormat, resolveManifestVideoBitrate(videoFormat))
    }

    override fun manipulate(data: EventData) {
        applyVideoFormat(data, videoFormatHolder)
        applyAudioFormat(data, currentAudioFormat)
    }

    // Looks the currently playing format up in the current video track group by its id to report the
    // manifest bitrate, falling back to the format's own bitrate when no manifest match exists
    // (e.g. progressive streams or before the manifest tracks are available).
    @androidx.annotation.OptIn(UnstableApi::class)
    private fun resolveManifestVideoBitrate(videoFormat: Format?): Int? {
        val formatId = videoFormat?.id ?: return videoFormat?.bitrate

        val manifestFormat =
            exoplayer.currentTracks.groups
                // Keep only video track groups; audio/text groups can't hold the video format we're after.
                .filter { it.type == C.TRACK_TYPE_VIDEO }
                // Flatten every group into its individual track formats (one per quality rung in the ladder).
                .flatMap { group -> (0 until group.length).map { group.getTrackFormat(it) } }
                // Match the playing format by id to find its manifest-declared counterpart.
                .firstOrNull { it.id == formatId }
        // Prefer the manifest bitrate from the matched track; fall back to the playing format's own bitrate
        // when there's no match (e.g. progressive streams or before manifest tracks are available).
        return manifestFormat?.bitrate ?: videoFormat.bitrate
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
        val oldFormat = videoFormatHolder?.currentVideoFormat
        return oldFormat == null ||
            newFormat.bitrate.toLong() != oldFormat.bitrate.toLong() ||
            newFormat.width != oldFormat.width ||
            newFormat.height != oldFormat.height
    }

    fun reset() {
        currentAudioFormat = null
        videoFormatHolder = null
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun applyVideoFormat(
        data: EventData,
        videoFormatHolder: VideoFormatHolder?,
    ) {
        val videoFormat = videoFormatHolder?.currentVideoFormat ?: return

        data.videoBitrate = videoFormatHolder.currentBitrateFromManifest ?: videoFormat.bitrate
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
        setVideoFormat(exoplayer.videoFormat)
        currentAudioFormat = exoplayer.audioFormat
    }
}

// Holds the currently playing video [Format] together with its manifest-declared bitrate. The
// bitrate of the [Format] reported by onVideoInputFormatChanged can reflect the actual media
// bitrate, which may differ from the bitrate declared in the manifest (observed for DASH, where the
// same representation is reported with its real media bitrate by the playing format but with its
// manifest `@bandwidth` in the track group from [ExoPlayer.getCurrentTracks]). The manifest bitrate
// is resolved once, when the format is captured, against the tracks active at that moment - resolving
// lazily on each sample would be racy with playlists, where the current tracks can already point to
// the next media item.
internal data class VideoFormatHolder(
    val currentVideoFormat: Format?,
    val currentBitrateFromManifest: Int?,
)
