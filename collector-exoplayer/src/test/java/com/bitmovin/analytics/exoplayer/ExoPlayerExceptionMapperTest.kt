package com.bitmovin.analytics.exoplayer

import com.google.android.exoplayer2.ExoPlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ExoPlayerExceptionMapperTest {
    @Test
    fun test_mapSourceError_toErrorCode() {
        // arrange
        val exoPlayerException =
            ExoPlaybackException.createForSource(
                IOException("test"),
                ExoPlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            )

        // act
        val errorCode = ExoPlayerExceptionMapper.map(exoPlayerException)

        // assert
        assertEquals(
            "Source Error: " + ExoPlaybackException.getErrorCodeName(ExoPlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND),
            errorCode.message,
        )
        assertEquals(ExoPlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND, errorCode.errorCode)
        assertEquals("Source error: null - java.io.IOException: test", errorCode.errorData.exceptionMessage)
    }

    @Test
    fun test_mapRemoteError_toErrorCode() {
        // arrange
        val exoPlayerException = ExoPlaybackException.createForRemote("remoteError")

        // act
        val errorCode = ExoPlayerExceptionMapper.map(exoPlayerException)

        // assert
        assertEquals(
            "Remote Error: " + ExoPlaybackException.getErrorCodeName(ExoPlaybackException.ERROR_CODE_REMOTE_ERROR),
            errorCode.message,
        )
        assertEquals(ExoPlaybackException.ERROR_CODE_REMOTE_ERROR, errorCode.errorCode)
        assertEquals("Remote error: remoteError", errorCode.errorData.exceptionMessage)
    }

    @Test
    fun test_mapRenderError_toErrorCode() {
        // arrange
        val exoPlayerException =
            ExoPlaybackException.createForRenderer(
                RuntimeException("test"),
                "Render123",
                0,
                null,
                0,
                false,
                ExoPlaybackException.ERROR_CODE_DECODING_FAILED,
            )

        // act
        val errorCode = ExoPlayerExceptionMapper.map(exoPlayerException)

        // assert
        assertEquals(
            "Render Error: " + ExoPlaybackException.getErrorCodeName(ExoPlaybackException.ERROR_CODE_DECODING_FAILED),
            errorCode.message,
        )
        assertEquals(ExoPlaybackException.ERROR_CODE_DECODING_FAILED, errorCode.errorCode)
        assertEquals(
            "Render123 error, index=0, format=null, format_supported=YES: null - java.lang.RuntimeException: test",
            errorCode.errorData.exceptionMessage,
        )
    }

    @Test
    fun test_mapUnexpectedError_toErrorCode() {
        // arrange
        val exoPlayerException =
            ExoPlaybackException.createForUnexpected(
                RuntimeException("test", IOException("testIO")),
                ExoPlaybackException.ERROR_CODE_UNSPECIFIED,
            )

        // act
        val errorCode = ExoPlayerExceptionMapper.map(exoPlayerException)

        // assert
        assertEquals(
            "Unexpected Error: " + ExoPlaybackException.getErrorCodeName(ExoPlaybackException.ERROR_CODE_UNSPECIFIED),
            errorCode.message,
        )
        assertEquals(ExoPlaybackException.ERROR_CODE_UNSPECIFIED, errorCode.errorCode)
        // for some reason a unexpected error get the message "Unexpected runtime error: null", seems to be a bug inside exoplayer to show null here
        assertEquals("Unexpected runtime error: null - java.lang.RuntimeException: test", errorCode.errorData.exceptionMessage)
    }
}
