package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.features.segmenttracking.SegmentTracking
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ErrorDetailTrackingTests {
    @Test
    fun testSuccessfullySubscribesToMultipleSources() {
        val backend = mockk<ErrorDetailBackend>(relaxed = true)
        val support1 = ObservableSupport<OnErrorDetailEventListener>()
        val support2 = ObservableSupport<OnErrorDetailEventListener>()
        val errorDetailTracking = ErrorDetailTracking(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), backend, null, support1, support2)
        mockkObject(errorDetailTracking)
        support1.notify { it.onError(null, null, ErrorData()) }
        support1.notify { it.onError(null, null, ErrorData()) }
        support2.notify { it.onError(null, null, ErrorData()) }
        support2.notify { it.onError(null, null, ErrorData()) }

        verify(exactly = 4) { errorDetailTracking.onError(any(), any(), any()) }
    }

    @Test
    fun testSuccessfullyUnsubscribesFromSourcesAndClearsBackendOnDisabling() {
        val support = ObservableSupport<OnErrorDetailEventListener>()
        val backend = mockk<ErrorDetailBackend>(relaxed = true)
        val errorDetailTracking = ErrorDetailTracking(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), backend, null, support)
        mockkObject(errorDetailTracking)
        support.notify { it.onError(null, null, ErrorData()) }
        verify { errorDetailTracking.onError(any(), any(), any()) }
        clearMocks(errorDetailTracking)
        errorDetailTracking.disable()
        verify { backend.clear() }
        support.notify { it.onError(null, null, ErrorData()) }
        verify(exactly = 0) { errorDetailTracking.onError(any(), any(), any()) }
    }

    @Test
    fun testEnablesAndFlushesBackendAfterEnablingFeature() {
        val support = ObservableSupport<OnErrorDetailEventListener>()
        val backend = mockk<ErrorDetailBackend>(relaxed = true)
        val errorDetailTracking = ErrorDetailTracking(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), backend, null, support)
        errorDetailTracking.enabled()
        verify { backend.enabled = true }
        verify { backend.flush() }
    }

    @Test
    fun testLimitsQueuedItemsAfterConfiguringFeature() {
        val support = ObservableSupport<OnErrorDetailEventListener>()
        val backend = mockk<ErrorDetailBackend>(relaxed = true)
        val segmentTracking = SegmentTracking()
        val errorDetailTracking = ErrorDetailTracking(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), backend, segmentTracking, support)
        errorDetailTracking.configured(true, ErrorDetailTrackingConfig(true, 100))
        verify { backend.limitSegmentsInQueue(segmentTracking.maxSegments) }
    }

    @Test
    fun testAddsSegmentsOnError() {
        val backend = mockk<ErrorDetailBackend>(relaxed = true)
        val segmentTracking = SegmentTracking()
        segmentTracking.onDownloadFinished(OnDownloadFinishedEventObject(mockk()))
        segmentTracking.onDownloadFinished(OnDownloadFinishedEventObject(mockk()))
        val errorDetailTracking = ErrorDetailTracking(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), backend, segmentTracking)
        errorDetailTracking.onError(null, null, ErrorData())
        val slot = slot<ErrorDetail>()
        verify { backend.send(capture(slot)) }
        assertThat(slot.captured.segments).isNotNull
        assertThat(slot.captured.segments?.size).isEqualTo(2)
    }

    @Test
    fun testDoesntAddSegmentsOnErrorIfSegmentTrackingIsDisabled() {
        val backend = mockk<ErrorDetailBackend>(relaxed = true)
        val segmentTracking = SegmentTracking()
        segmentTracking.onDownloadFinished(OnDownloadFinishedEventObject(mockk()))
        segmentTracking.onDownloadFinished(OnDownloadFinishedEventObject(mockk()))
        segmentTracking.disable()
        val errorDetailTracking = ErrorDetailTracking(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), backend, segmentTracking)
        errorDetailTracking.onError(null, null, ErrorData())
        val slot = slot<ErrorDetail>()
        verify { backend.send(capture(slot)) }
        assertThat(slot.captured.segments?.size ?: 0).isEqualTo(0)
    }
}
