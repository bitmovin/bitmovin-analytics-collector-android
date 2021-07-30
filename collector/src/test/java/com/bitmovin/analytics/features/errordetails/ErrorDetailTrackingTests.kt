package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.features.httprequesttracking.HttpRequestTracking
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
        val httpRequestTracking = HttpRequestTracking()
        val errorDetailTracking = ErrorDetailTracking(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), backend, httpRequestTracking, support)
        errorDetailTracking.configured(true, ErrorDetailTrackingConfig(true, 100))
        verify { backend.limitHttpRequestsInQueue(httpRequestTracking.maxRequests) }
    }

    @Test
    fun testAddsHttpRequestsOnError() {
        val backend = mockk<ErrorDetailBackend>(relaxed = true)
        val httpRequestTracking = HttpRequestTracking()
        httpRequestTracking.onDownloadFinished(OnDownloadFinishedEventObject(mockk()))
        httpRequestTracking.onDownloadFinished(OnDownloadFinishedEventObject(mockk()))
        val errorDetailTracking = ErrorDetailTracking(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), backend, httpRequestTracking)
        errorDetailTracking.onError(null, null, ErrorData())
        val slot = slot<ErrorDetail>()
        verify { backend.send(capture(slot)) }
        assertThat(slot.captured.httpRequests).isNotNull
        assertThat(slot.captured.httpRequests?.size).isEqualTo(2)
    }

    @Test
    fun testDoesntAddHttpRequestsOnErrorIfHttpReqeustTrackingTrackingIsDisabled() {
        val backend = mockk<ErrorDetailBackend>(relaxed = true)
        val httpRequestTracking = HttpRequestTracking()
        httpRequestTracking.onDownloadFinished(OnDownloadFinishedEventObject(mockk()))
        httpRequestTracking.onDownloadFinished(OnDownloadFinishedEventObject(mockk()))
        httpRequestTracking.disable()
        val errorDetailTracking = ErrorDetailTracking(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), backend, httpRequestTracking)
        errorDetailTracking.onError(null, null, ErrorData())
        val slot = slot<ErrorDetail>()
        verify { backend.send(capture(slot)) }
        assertThat(slot.captured.httpRequests?.size ?: 0).isEqualTo(0)
    }
}
