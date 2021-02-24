package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.features.segmenttracking.Segment
import com.bitmovin.analytics.features.segmenttracking.SegmentType
import com.bitmovin.analytics.utils.HttpClient
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ErrorDetailBackendTests {
    @Before
    fun setup() {
        mockkConstructor(HttpClient::class)
    }

    @Test
    fun testCorrectlyLimitsSegmentsInQueue() {
        val backend = ErrorDetailBackend(mockk())
        val d1 = getErrorDetail(5)
        val d2 = getErrorDetail(5)
        val d3 = getErrorDetail(null)
        backend.send(d1)
        backend.send(d2)
        backend.send(d3)
        backend.limitSegmentsInQueue(2)
        assertThat(d1.segments?.size).isLessThanOrEqualTo(2)
        assertThat(d2.segments?.size).isLessThanOrEqualTo(2)
        assertThat(d3.segments?.size).isNull()
    }

    @Test
    fun testWontPostWhileDisabled() {
        val backend = ErrorDetailBackend(mockk())
        backend.send(getErrorDetail(0))
        verify(exactly = 0) { anyConstructed<HttpClient>().post(any(), any(), any()) }
    }

    @Test
    fun testWillPostWhenEnabled() {
        val backend = ErrorDetailBackend(mockk(relaxed = true))
        backend.enabled = true
        backend.send(getErrorDetail(0))
        verify(exactly = 1) { anyConstructed<HttpClient>().post(any(), any(), any()) }
    }

    @Test
    fun testWillFlushToHttpClientIfEnabled() {
        val backend = ErrorDetailBackend(mockk(relaxed = true))
        backend.send(getErrorDetail(0))
        backend.enabled = true
        verify(exactly = 0) { anyConstructed<HttpClient>().post(any(), any(), any()) }
        backend.flush()
        verify(exactly = 1) { anyConstructed<HttpClient>().post(any(), any(), any()) }
    }

    @Test
    fun testWillRequeueOnFlushIfDisabled() {
        val backend = ErrorDetailBackend(mockk(relaxed = true))
        backend.send(getErrorDetail(0))
        verify(exactly = 0) { anyConstructed<HttpClient>().post(any(), any(), any()) }
        backend.flush()
        verify(exactly = 0) { anyConstructed<HttpClient>().post(any(), any(), any()) }
    }

    private fun getErrorDetail(segmentCount: Int?) = ErrorDetail(0, null, null, null, if(segmentCount == null) null else (0..segmentCount).map { getSegment() }.toMutableList())
    private fun getSegment() = Segment(0, SegmentType.MANIFEST_DASH, null, null , 0, 0.0, 0, true)
}
