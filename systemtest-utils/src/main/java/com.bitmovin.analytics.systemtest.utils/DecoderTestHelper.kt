package com.bitmovin.analytics.systemtest.utils

// copied from player folks
// https://github.com/bitmovin-engineering/player-android/blob/main/player-core/src/androidTest/java/com/bitmovin/player/tests/player/DecoderTestHelper.kt
import android.media.MediaCodec
import android.media.MediaFormat
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/** Create all possible decoders matching [format] then run [block]. */
suspend fun noAvailableDecoder(
    format: MediaFormat,
    block: suspend () -> Unit,
) {
    val codecs = mutableListOf<MediaCodec>()
    try {
        while (currentCoroutineContext().isActive) {
            var codec: MediaCodec? = null
            try {
                codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
                codec.configure(format, null, null, 0)
                codec.start()
            } catch (_: Exception) {
                codec?.release()
                break
            }
            codecs.add(codec)
        }
        block()
    } finally {
        codecs.forEach(MediaCodec::release)
    }
}
