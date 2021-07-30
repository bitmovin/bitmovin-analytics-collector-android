package com.bitmovin.analytics.features.httprequesttracking

import com.bitmovin.analytics.ObservableSupport
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HttpRequestTrackingTests {
    @Test
    fun testSuccessfullyUsesDefaultConfigurationValuesIfNoConfigurationIsApplied() {
        val segmentTracking = SegmentTracking()
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
        assertThat(segmentTracking.httpRequests.size).isEqualTo(4)
    }

    @Test
    fun testSuccessfullyAddsSegmentsAndLimitsQueue() {
        val support = ObservableSupport<OnDownloadFinishedEventListener>()
        val segmentTracking = SegmentTracking(support)
        segmentTracking.configure(3)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.httpRequests.size).isEqualTo(3)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.httpRequests.size).isEqualTo(3)
    }

    @Test
    fun testSuccessfullyLimitsQueueOnConfiguring() {
        val support = ObservableSupport<OnDownloadFinishedEventListener>()
        val segmentTracking = SegmentTracking(support)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.httpRequests.size).isEqualTo(4)
        segmentTracking.configure(3)
        assertThat(segmentTracking.httpRequests.size).isEqualTo(3)
    }

    @Test
    fun testSuccessfullyRemovesSourcesAndClearsQueueOnDisabling() {
        val support = ObservableSupport<OnDownloadFinishedEventListener>()
        val segmentTracking = SegmentTracking(support)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.httpRequests.size).isEqualTo(2)
        segmentTracking.disable()
        assertThat(segmentTracking.httpRequests.size).isEqualTo(0)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(segmentTracking.httpRequests.size).isEqualTo(0)
    }
}
