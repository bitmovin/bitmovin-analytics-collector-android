package com.bitmovin.analytics.error

import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ErrorReportingLimiterTest {
    @Test
    fun testErrorReportingLimiter_ShouldStopReportingAfter5IdenticalErrors() {
        val identicalErrorReportingLimiter = IdenticalErrorReportingLimiter()

        for (i in 1..5) {
            assertThat(
                identicalErrorReportingLimiter.shouldReportError(
                    ErrorCode(
                        10,
                        "testMessage",
                        ErrorData(),
                    ),
                ),
            ).isTrue
        }

        assertThat(
            identicalErrorReportingLimiter.shouldReportError(
                ErrorCode(
                    10,
                    "testMessage",
                    ErrorData(),
                ),
            ),
        ).isFalse
    }

    @Test
    fun testErrorReportingLimiter_ShouldReportNewErrors() {
        val identicalErrorReportingLimiter = IdenticalErrorReportingLimiter()

        for (i in 1..5) {
            assertThat(
                identicalErrorReportingLimiter.shouldReportError(
                    ErrorCode(
                        10,
                        "testMessage1",
                        ErrorData(),
                    ),
                ),
            ).isTrue
        }

        assertThat(
            identicalErrorReportingLimiter.shouldReportError(
                ErrorCode(
                    5,
                    "testMessage2",
                    ErrorData(),
                ),
            ),
        ).isTrue
    }

    @Test
    fun testErrorReportingLimiter_ShouldReportAllAlternatingErrors() {
        val identicalErrorReportingLimiter = IdenticalErrorReportingLimiter()

        // two alternating errors (which is not blocked)
        for (i in 1..10) {
            assertThat(
                identicalErrorReportingLimiter.shouldReportError(
                    ErrorCode(
                        10,
                        "testMessage1",
                        ErrorData(),
                    ),
                ),
            ).isTrue
            assertThat(
                identicalErrorReportingLimiter.shouldReportError(
                    ErrorCode(
                        5,
                        "testMessage2",
                        ErrorData(),
                    ),
                ),
            ).isTrue
        }
    }

    @Test
    fun testErrorReportingLimiter_ShouldReportAllErrorsWithAlternatingSeverity() {
        val identicalErrorReportingLimiter = IdenticalErrorReportingLimiter()

        // two alternating errors (which is not blocked)
        for (i in 1..10) {
            assertThat(
                identicalErrorReportingLimiter.shouldReportError(
                    ErrorCode(
                        10,
                        "testMessage1",
                        ErrorData(),
                        errorSeverity = ErrorSeverity.CRITICAL,
                    ),
                ),
            ).isTrue
            assertThat(
                identicalErrorReportingLimiter.shouldReportError(
                    ErrorCode(
                        10,
                        "testMessage1",
                        ErrorData(),
                        errorSeverity = ErrorSeverity.INFO,
                    ),
                ),
            ).isTrue
        }
    }

    @Test
    fun testErrorReportingLimiter_ShouldReportSameErrorAgainAfterClearing() {
        val identicalErrorReportingLimiter = IdenticalErrorReportingLimiter()

        for (i in 1..5) {
            assertThat(
                identicalErrorReportingLimiter.shouldReportError(
                    ErrorCode(
                        10,
                        "testMessage",
                        ErrorData(),
                    ),
                ),
            ).isTrue
        }

        identicalErrorReportingLimiter.reset()

        assertThat(
            identicalErrorReportingLimiter.shouldReportError(
                ErrorCode(
                    10,
                    "testMessage",
                    ErrorData(),
                ),
            ),
        ).isTrue
    }
}
