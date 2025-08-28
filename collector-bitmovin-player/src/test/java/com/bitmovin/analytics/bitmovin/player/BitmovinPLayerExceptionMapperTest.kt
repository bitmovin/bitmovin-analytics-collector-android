package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.player.api.deficiency.ErrorCode
import com.bitmovin.player.api.deficiency.ErrorEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class BitmovinPLayerExceptionMapperTest {
    @Test
    fun test_mapSourceError_toErrorCode() {
        val sourceErrorCode = ErrorCode.fromValue(2001) // Example source error code

        val errorEvent = mockk<ErrorEvent>()
        every { errorEvent.code }.returns(sourceErrorCode)
        every { errorEvent.message }.returns("Source error occurred")
        every { errorEvent.data }.returns(IOException("test exception message", RuntimeException("test exception cause")))

        // act
        val exceptionMapper = BitmovinPlayerExceptionMapper()
        val errorCode = exceptionMapper.map(errorEvent)

        // assert
        assertEquals("Source error occurred", errorCode.message)
        assertEquals(2001, errorCode.errorCode)
        assertEquals("test exception message", errorCode.errorData.exceptionMessage)

        // this verifies that the gson serialization works
        assertTrue(errorCode.errorData.additionalData?.contains("test exception cause")!!)
    }
}
