package com.bitmovin.analytics.error

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ErrorReportingLimiterTest {
    @Test
    fun testErrorReportingLimiter_ShouldStopReportingAfter5IdenticalErrors() {
        val identicalErrorReportingLimiter = IdenticalErrorReportingLimiter()

        for (i in 1..5) {
            assertThat(identicalErrorReportingLimiter.shouldReportError(10)).isTrue
        }

        assertThat(identicalErrorReportingLimiter.shouldReportError(10)).isFalse
    }

    @Test
    fun testErrorReportingLimiter_ShouldReportNewErrors() {
        val identicalErrorReportingLimiter = IdenticalErrorReportingLimiter()

        for (i in 1..5) {
            assertThat(identicalErrorReportingLimiter.shouldReportError(10)).isTrue
        }

        assertThat(identicalErrorReportingLimiter.shouldReportError(5)).isTrue
    }

    @Test
    fun testErrorReportingLimiter_ShouldReportAllAlternatingErrors() {
        val identicalErrorReportingLimiter = IdenticalErrorReportingLimiter()

        // two alternating errors (which is not blocked)
        for (i in 1..10) {
            assertThat(identicalErrorReportingLimiter.shouldReportError(10)).isTrue
            assertThat(identicalErrorReportingLimiter.shouldReportError(5)).isTrue
        }
    }

    @Test
    fun testErrorReportingLimiter_ShouldReportSameErrorAgainAfterClearing() {
        val identicalErrorReportingLimiter = IdenticalErrorReportingLimiter()

        for (i in 1..5) {
            assertThat(identicalErrorReportingLimiter.shouldReportError(10)).isTrue
        }

        identicalErrorReportingLimiter.reset()

        assertThat(identicalErrorReportingLimiter.shouldReportError(10)).isTrue
    }
}
