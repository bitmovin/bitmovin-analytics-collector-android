package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.features.segmenttracking.Segment
import com.bitmovin.analytics.features.segmenttracking.SegmentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ErrorDetailTrackingTests {
    @Test
    fun testErrorDetailLimitSegmentsShouldntFailIfSegmentsAreNull() {
        val errorDetail = ErrorDetail(0, null, null, null, null)
        errorDetail.limitSegments(1)
    }

    @Test
    fun testErrorDetailLimitSegmentsShouldLimitSegments() {
        val segment1 = Segment(0, SegmentType.MANIFEST_DASH, null, null, 0, 0.0, 0, true)
        val segment2 = Segment(0, SegmentType.MANIFEST_DASH, null, null, 0, 0.0, 0, true)
        val errorDetail = ErrorDetail(0, null, null, null, mutableListOf(segment1, segment2))
        assertThat(errorDetail.segments?.size).isEqualTo(2)
        errorDetail.limitSegments(1)
        assertThat(errorDetail.segments?.size).isEqualTo(1)
    }

    @Test
    fun testErrorDetailLimitSegmentsShouldRemoveItemsFromEnd() {
        val segment1 = Segment(0, SegmentType.MANIFEST_DASH, null, null, 0, 0.0, 0, true)
        val segment2 = Segment(1, SegmentType.MANIFEST_DASH, null, null, 0, 0.0, 0, true)
        val errorDetail = ErrorDetail(0, null, null, null, mutableListOf(segment1, segment2))
        errorDetail.limitSegments(1)
        assertThat(errorDetail.segments?.get(0)).isEqualTo(segment1)
        assertThat(errorDetail.segments?.get(0)).isNotEqualTo(segment2)
    }
}
