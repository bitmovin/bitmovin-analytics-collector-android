package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.CollectorConfig
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend.Companion.copyTruncateSegments
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend.Companion.copyTruncateStringsAndUrls
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
        val backend = ErrorDetailBackend(mockk(relaxed = true), mockk())
        val d1 = getErrorDetail(5)
        val d2 = getErrorDetail(5)
        val d3 = getErrorDetail(null)
        backend.send(d1)
        backend.send(d2)
        backend.send(d3)
        backend.limitSegmentsInQueue(2)
        assertThat(backend.queue.getOrNull(0)?.segments?.size).isLessThanOrEqualTo(2)
        assertThat(backend.queue.getOrNull(1)?.segments?.size).isLessThanOrEqualTo(2)
        assertThat(backend.queue.getOrNull(2)?.segments?.size).isNull()
    }

    @Test
    fun testErrorDetailLimitSegmentsShouldntFailIfSegmentsAreNull() {
        val errorDetail = ErrorDetail("", "", "", "", 0, 0, null, null, null, null)
        errorDetail.copyTruncateSegments(1)
    }

    @Test
    fun testErrorDetailLimitSegmentsShouldLimitSegments() {
        val segment1 = Segment(0, SegmentType.MANIFEST_DASH, null, null, 0, 0L, null, 0, true)
        val segment2 = Segment(0, SegmentType.MANIFEST_DASH, null, null, 0, 0L, null, 0, true)
        val errorDetail = ErrorDetail("", "", "", "", 0, 0, null, null, null, mutableListOf(segment1, segment2))
        assertThat(errorDetail.segments?.size).isEqualTo(2)
        val copy = errorDetail.copyTruncateSegments(1)
        assertThat(copy.segments?.size).isEqualTo(1)
    }

    @Test
    fun testErrorDetailCopyTruncateStringsAndUrlsShouldCorrectlyTruncateStringsAndUrls() {
        val segment1 = Segment(0, SegmentType.MANIFEST_DASH, "0123456789", "0123456789", 0, 0L, null, 0, true)
        val segment2 = Segment(0, SegmentType.MANIFEST_DASH, null, "0123", 0, 0L, null, 0, true)
        val errorDetail = ErrorDetail("", "", "", "", 0, 0, null, "0123456789", null, mutableListOf(segment1, segment2))
        val copy = errorDetail.copyTruncateStringsAndUrls(5, 5)
        assertThat(copy.analyticsVersion).isEqualTo(errorDetail.analyticsVersion)
        assertThat(copy.code).isEqualTo(errorDetail.code)
        assertThat(copy.domain).isEqualTo(errorDetail.domain)
        assertThat(copy.errorId).isEqualTo(errorDetail.errorId)
        assertThat(copy.impressionId).isEqualTo(errorDetail.impressionId)
        assertThat(copy.licenseKey).isEqualTo(errorDetail.licenseKey)
        assertThat(copy.message).isEqualTo("01234")
        assertThat(copy.platform).isEqualTo(errorDetail.platform)
        assertThat(copy.segments?.size).isEqualTo(errorDetail.segments?.size)
        assertThat(copy.segments?.get(0)?.url).isEqualTo("01234")
        assertThat(copy.segments?.get(0)?.lastRedirectLocation).isEqualTo("01234")
        assertThat(copy.segments?.get(1)?.url).isEqualTo(null)
        assertThat(copy.segments?.get(1)?.lastRedirectLocation).isEqualTo("0123")
    }

    @Test
    fun testErrorDetailCopyTruncateStringsAndUrlsShouldCorrectlyTruncateStringsAndUrlsWithNullSegments() {
        val errorDetail = ErrorDetail("", "", "", "", 0, 0, null, "0123456789", null, null)
        val copy = errorDetail.copyTruncateStringsAndUrls(5, 5)
        assertThat(copy.analyticsVersion).isEqualTo(errorDetail.analyticsVersion)
        assertThat(copy.code).isEqualTo(errorDetail.code)
        assertThat(copy.domain).isEqualTo(errorDetail.domain)
        assertThat(copy.errorId).isEqualTo(errorDetail.errorId)
        assertThat(copy.impressionId).isEqualTo(errorDetail.impressionId)
        assertThat(copy.licenseKey).isEqualTo(errorDetail.licenseKey)
        assertThat(copy.message).isEqualTo("01234")
        assertThat(copy.platform).isEqualTo(errorDetail.platform)
        assertThat(copy.segments?.size).isEqualTo(errorDetail.segments?.size)
    }

    @Test
    fun testErrorDetailLimitSegmentsShouldRemoveItemsFromEnd() {
        val segment1 = Segment(0, SegmentType.MANIFEST_DASH, null, null, 0, 0L, null, 0, true)
        val segment2 = Segment(1, SegmentType.MANIFEST_DASH, null, null, 0, 0L, null, 0, true)
        val errorDetail = ErrorDetail("", "", "", "", 0, 0, null, null, null, mutableListOf(segment1, segment2))
        errorDetail.copyTruncateSegments(1)
        assertThat(errorDetail.segments?.get(0)).isEqualTo(segment1)
        assertThat(errorDetail.segments?.get(0)).isNotEqualTo(segment2)
    }

    @Test
    fun testWontPostWhileDisabled() {
        val backend = ErrorDetailBackend(mockk(relaxed = true), mockk())
        backend.send(getErrorDetail(0))
        verify(exactly = 0) { anyConstructed<HttpClient>().post(any(), any(), any()) }
    }

    @Test
    fun testWillPostWhenEnabled() {
        val backend = ErrorDetailBackend(CollectorConfig().also { it.backendUrl = "http://localhost" }, mockk(relaxed = true))
        backend.enabled = true
        backend.send(getErrorDetail(0))
        verify(exactly = 1) { anyConstructed<HttpClient>().post(any(), any(), any()) }
    }

    @Test
    fun testWillFlushToHttpClientIfEnabled() {
        val backend = ErrorDetailBackend(CollectorConfig().also { it.backendUrl = "http://localhost" }, mockk(relaxed = true))
        backend.send(getErrorDetail(0))
        backend.enabled = true
        verify(exactly = 0) { anyConstructed<HttpClient>().post(any(), any(), any()) }
        backend.flush()
        verify(exactly = 1) { anyConstructed<HttpClient>().post(any(), any(), any()) }
    }

    @Test
    fun testWillRequeueOnFlushIfDisabled() {
        val backend = ErrorDetailBackend(mockk(relaxed = true), mockk(relaxed = true))
        backend.send(getErrorDetail(0))
        verify(exactly = 0) { anyConstructed<HttpClient>().post(any(), any(), any()) }
        backend.flush()
        verify(exactly = 0) { anyConstructed<HttpClient>().post(any(), any(), any()) }
    }

    private fun getErrorDetail(segmentCount: Int?) = ErrorDetail("platform", "key", "domain", "impressionId", 0, 0, null, null, null, if (segmentCount == null) null else (0..segmentCount).map { getSegment() }.toMutableList())
    private fun getSegment() = Segment(0, SegmentType.MANIFEST_DASH, null, null, 0, 0L, null, 0, true)
}
