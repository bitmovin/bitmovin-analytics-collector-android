package com.bitmovin.analytics.features.httprequesttracking

import com.bitmovin.analytics.ObservableSupport
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class HttpRequestTrackingTests {
    @Test
    fun testSuccessfullyUsesDefaultConfigurationValuesIfNoConfigurationIsApplied() {
        val httpRequestTracking = HttpRequestTracking()
        assertThat(httpRequestTracking.maxRequests).isEqualTo(HttpRequestTracking.defaultMaxRequests)
    }

    @Test
    fun testSuccessfullyAddsHttpRequestsFromMultipleSources() {
        val support1 = ObservableSupport<OnDownloadFinishedEventListener>()
        val support2 = ObservableSupport<OnDownloadFinishedEventListener>()
        val httpRequestTracking = HttpRequestTracking(support1, support2)
        support1.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support1.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support2.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support2.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(httpRequestTracking.httpRequests.size).isEqualTo(4)
    }

    @Test
    fun testSuccessfullyAddsHttpRequestsAndLimitsQueue() {
        val support = ObservableSupport<OnDownloadFinishedEventListener>()
        val httpRequestTracking = HttpRequestTracking(support)
        httpRequestTracking.configure(3)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(httpRequestTracking.httpRequests.size).isEqualTo(3)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(httpRequestTracking.httpRequests.size).isEqualTo(3)
    }

    @Test
    fun testSuccessfullyLimitsQueueOnConfiguring() {
        val support = ObservableSupport<OnDownloadFinishedEventListener>()
        val httpRequestTracking = HttpRequestTracking(support)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(httpRequestTracking.httpRequests.size).isEqualTo(4)
        httpRequestTracking.configure(3)
        assertThat(httpRequestTracking.httpRequests.size).isEqualTo(3)
    }

    @Test
    fun testSuccessfullyRemovesSourcesAndClearsQueueOnDisabling() {
        val support = ObservableSupport<OnDownloadFinishedEventListener>()
        val httpRequestTracking = HttpRequestTracking(support)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(httpRequestTracking.httpRequests.size).isEqualTo(2)
        httpRequestTracking.disable()
        assertThat(httpRequestTracking.httpRequests.size).isEqualTo(0)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(httpRequestTracking.httpRequests.size).isEqualTo(0)
    }

    @Test
    fun testSuccessfullyAddsRequestsAfterQueueIsCleared() {
        val support = ObservableSupport<OnDownloadFinishedEventListener>()
        val httpRequestTracking = HttpRequestTracking(support)
        httpRequestTracking.configure(3)
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(httpRequestTracking.httpRequests.size).isEqualTo(3)
        httpRequestTracking.reset()
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        support.notify { it.onDownloadFinished(OnDownloadFinishedEventObject(mockk())) }
        assertThat(httpRequestTracking.httpRequests.size).isEqualTo(3)
    }
}
