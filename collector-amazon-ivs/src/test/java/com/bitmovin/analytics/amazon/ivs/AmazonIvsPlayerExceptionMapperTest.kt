package com.bitmovin.analytics.amazon.ivs

import com.amazonaws.ivs.player.ErrorType
import com.amazonaws.ivs.player.PlayerException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AmazonIvsPlayerExceptionMapperTest {
    @Test
    fun testMap_ShouldMapExceptionFieldsCorrectly() {
        // arrange
        val pe = mockk<PlayerException>(relaxed = true)
        every { pe.errorType.errorCode }.returns(ErrorType.ERROR_INVALID_DATA.errorCode)
        every { pe.source }.returns("testSource")
        every { pe.errorType }.returns(ErrorType.ERROR_INVALID_DATA)
        every { pe.message }.returns("testMessage")
        every { pe.errorMessage }.returns("errorMessage")

        val mapper = AmazonIvsPlayerExceptionMapper()

        // act
        val errorCode = mapper.map(pe)

        // assert
        assertThat(errorCode.errorCode).isEqualTo(ErrorType.ERROR_INVALID_DATA.errorCode)
        assertThat(errorCode.description).isEqualTo(ErrorType.ERROR_INVALID_DATA.name)
        assertThat(errorCode.legacyErrorData).isNull()
        assertThat(errorCode.errorData.exceptionMessage).isEqualTo("testMessage")
    }
}
