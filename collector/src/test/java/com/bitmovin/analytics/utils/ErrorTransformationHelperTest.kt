package com.bitmovin.analytics.utils

import com.bitmovin.analytics.api.error.AnalyticsError
import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.dtos.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ErrorTransformationHelperTest {
    @Test
    fun `transformErrorWithUserCallback should call the callback`() {
        // arrange
        val originalErrorCode =
            ErrorCode(
                errorCode = 100,
                message = "Original error",
                errorData = com.bitmovin.analytics.dtos.ErrorData(),
                errorSeverity = ErrorSeverity.CRITICAL,
            )

        // act
        val transformedError =
            ErrorTransformationHelper.transformErrorWithUserCallback(
                errorTransformerCallback = { analyticsError, context ->
                    AnalyticsError(
                        code = analyticsError.code + 10,
                        message = "Transformed: ${analyticsError.message}",
                        severity = ErrorSeverity.INFO,
                    )
                },
                errorCode = originalErrorCode,
                originalError = Throwable("Connection failed"),
            )

        // assert
        assertThat(transformedError.errorCode).isEqualTo(110)
        assertThat(transformedError.message).isEqualTo("Transformed: Original error")
        assertThat(transformedError.errorSeverity).isEqualTo(ErrorSeverity.INFO)
    }

    @Test
    fun `transformErrorWithUserCallback should return same error object when no callback is set`() {
        // arrange
        val originalErrorCode =
            ErrorCode(
                errorCode = 100,
                message = "Original error",
                errorData = com.bitmovin.analytics.dtos.ErrorData(),
                errorSeverity = ErrorSeverity.CRITICAL,
            )

        // act
        val transformedError =
            ErrorTransformationHelper.transformErrorWithUserCallback(
                errorTransformerCallback = null,
                errorCode = originalErrorCode,
                originalError = Throwable("Connection failed"),
            )

        // assert
        assertThat(transformedError.errorCode).isEqualTo(100)
        assertThat(transformedError.message).isEqualTo("Original error")
        assertThat(transformedError.errorSeverity).isEqualTo(ErrorSeverity.CRITICAL)
    }
}
