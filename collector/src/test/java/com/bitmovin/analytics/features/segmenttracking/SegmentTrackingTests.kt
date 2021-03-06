package com.bitmovin.analytics.features.segmenttracking

import com.bitmovin.analytics.ObservableSupport
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SegmentTrackingTests {
    @Test
    fun testSuccessfullyAppliesConfigurationValues() {
        val segmentTracking = SegmentTracking()
        val maxSegments = 123
        segmentTracking.configure(true, "{\"maxSegments\": $maxSegments}")
        assertThat(segmentTracking.config).isNotNull
        assertThat(segmentTracking.maxSegments).isEqualTo(maxSegments)
    }

    @Test
    fun testSuccessfullyUsesDefaultConfigurationValuesForMissingValues() {
        val segmentTracking = SegmentTracking()
        segmentTracking.configure(true, "{}")
        assertThat(segmentTracking.config).isNotNull
        assertThat(segmentTracking.maxSegments).isEqualTo(SegmentTracking.defaultMaxSegments)
    }

    @Test
    fun testSuccessfullyUsesDefaultConfigurationValuesIfNoConfigurationIsApplied() {
        val segmentTracking = SegmentTracking()
        segmentTracking.configure(true, null)
        assertThat(segmentTracking.config).isNull()
        assertThat(segmentTracking.maxSegments).isEqualTo(SegmentTracking.defaultMaxSegments)
    }

    @Test
    fun testSuccessfullyAddsSegmentsFromMultipleSources() {
        val support1 = ObservableSupport<OnDownloadFinishedEventListener>()
        val support2 = ObservableSupport<OnDownloadFinishedEventListener>()
        val segmentTracking = SegmentTracking(support1, support2)
        support1.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support1.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support2.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support2.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.segments.size).isEqualTo(4)
    }

    @Test
    fun testSuccessfullyAddsSegmentsAndLimitsQueue() {
        val support = ObservableSupport<OnDownloadFinishedEventListener>()
        val segmentTracking = SegmentTracking(support)
        segmentTracking.configure(true, "{\"maxSegments\": 3}")
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.segments.size).isEqualTo(3)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.segments.size).isEqualTo(3)
    }

    @Test
    fun testSuccessfullyLimitsQueueOnConfiguring() {
        val support = ObservableSupport<OnDownloadFinishedEventListener>()
        val segmentTracking = SegmentTracking(support)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.segments.size).isEqualTo(4)
        segmentTracking.configure(true, "{\"maxSegments\": 3}")
        assertThat(segmentTracking.segments.size).isEqualTo(3)
    }

    @Test
    fun testSuccessfullyRemovesSourcesAndClearsQueueOnDisabling() {
        val support = ObservableSupport<OnDownloadFinishedEventListener>()
        val segmentTracking = SegmentTracking(support)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.segments.size).isEqualTo(2)
        segmentTracking.disable()
        assertThat(segmentTracking.segments.size).isEqualTo(0)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.segments.size).isEqualTo(0)
    }
}
