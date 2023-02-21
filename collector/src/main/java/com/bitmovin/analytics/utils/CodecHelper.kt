package com.bitmovin.analytics.utils

class CodecHelper {

    companion object {
        private val VIDEO_CODECS = arrayListOf("avc", "hvc1", "av01", "av1", "hev1", "vp9", "hevc", "mpeg4")

        // ac-3 -> Dolby Digital
        // ec-3 -> Dolby Digital plus
        // vorbis, opus -> open source codecs
        private val AUDIO_CODECS = arrayListOf("mp4a", "ec-3", "ac-3", "opus", "vorbis")

        fun isVideoCodec(codec: String): Boolean = codec.isNotEmpty() && VIDEO_CODECS.any { codec.startsWith(it) }
        fun isAudioCodec(codec: String): Boolean = codec.isNotEmpty() && AUDIO_CODECS.any { codec.startsWith(it) }
    }
}
