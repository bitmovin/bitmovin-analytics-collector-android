package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.dtos.ErrorDetail
import com.bitmovin.analytics.dtos.HttpRequest
import com.bitmovin.analytics.dtos.HttpRequestType
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend.Companion.copyTruncateHttpRequests
import com.bitmovin.analytics.features.errordetails.ErrorDetailBackend.Companion.copyTruncateStringsAndUrls
import com.bitmovin.analytics.utils.HttpClient
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ErrorDetailBackendTests {
    lateinit var httpClientMock: HttpClient

    @Before
    fun setup() {
        httpClientMock = mockk(relaxed = true)
    }

    @Test
    fun testCorrectlyLimitsHttpRequestsInQueue() {
        val backend = ErrorDetailBackend(mockk(relaxed = true), mockk(), httpClientMock)
        val d1 = getErrorDetail(5)
        val d2 = getErrorDetail(5)
        val d3 = getErrorDetail(null)
        backend.send(d1)
        backend.send(d2)
        backend.send(d3)
        backend.limitHttpRequestsInQueue(2)
        assertThat(backend.queue.getOrNull(0)?.httpRequests?.size).isLessThanOrEqualTo(2)
        assertThat(backend.queue.getOrNull(1)?.httpRequests?.size).isLessThanOrEqualTo(2)
        assertThat(backend.queue.getOrNull(2)?.httpRequests?.size).isNull()
    }

    @Test
    fun testDecodingErrorsAreCutOffAfter4500Characters() {
        val backend = ErrorDetailBackend(mockk(relaxed = true), mockk(), httpClientMock)
        val d1 =
            ErrorDetail(
                platform = "platform",
                licenseKey = "test",
                domain = "domain",
                impressionId = "impressionId",
                errorId = 0,
                timestamp = 0,
                // decoding error
                code = 2100,
                message = null,
                data = ErrorData(additionalData = "a".repeat(5000)),
                httpRequests = null,
            )

        // act
        backend.send(d1)

        // assert
        assertThat(backend.queue[0].data.additionalData?.length).isEqualTo(4500)
    }

    @Test
    fun testNonDecodingErrorsAreCutOffAfter2000Characters() {
        val backend = ErrorDetailBackend(mockk(relaxed = true), mockk(), httpClientMock)
        val d1 =
            ErrorDetail(
                platform = "platform",
                licenseKey = "test",
                domain = "domain",
                impressionId = "impressionId",
                errorId = 0,
                timestamp = 0,
                // NOT decoding error
                code = 100,
                message = null,
                data = ErrorData(additionalData = "a".repeat(5000)),
                httpRequests = null,
            )

        // act
        backend.send(d1)

        // assert
        assertThat(backend.queue[0].data.additionalData?.length).isEqualTo(2000)
    }

    @Test
    fun testErrorDetailLimitHttpRequestsShouldntFailIfHttpRequestsAreNull() {
        val errorDetail = ErrorDetail("", "", "", "", 0, 0, null, null, ErrorData(), null)
        errorDetail.copyTruncateHttpRequests(1)
    }

    @Test
    fun testErrorDetailLimitHttpRequestsShouldLimitHttpRequests() {
        val httpRequest1 = HttpRequest(0, HttpRequestType.MANIFEST_DASH, null, null, 0, 0L, null, 0, true)
        val httpRequest2 = HttpRequest(0, HttpRequestType.MANIFEST_DASH, null, null, 0, 0L, null, 0, true)
        val errorDetail = ErrorDetail("", "", "", "", 0, 0, null, null, ErrorData(), mutableListOf(httpRequest1, httpRequest2))
        assertThat(errorDetail.httpRequests?.size).isEqualTo(2)
        val copy = errorDetail.copyTruncateHttpRequests(1)
        assertThat(copy.httpRequests?.size).isEqualTo(1)
    }

    @Test
    fun testErrorDetailCopyTruncateStringsAndUrlsShouldCorrectlyTruncateStringsAndUrls() {
        val longString = "a".repeat(4600)

        val httpRequest1 = HttpRequest(0, HttpRequestType.MANIFEST_DASH, longString, longString, 0, 0L, null, 0, true)
        val httpRequest2 = HttpRequest(0, HttpRequestType.MANIFEST_DASH, null, "0123", 0, 0L, null, 0, true)
        val errorDetail =
            ErrorDetail(
                "", "", "", "", 0, 0, null, longString,
                ErrorData(
                    additionalData = longString,
                ),
                mutableListOf(httpRequest1, httpRequest2),
            )
        val copy = errorDetail.copyTruncateStringsAndUrls(4000)
        assertThat(copy.analyticsVersion).isEqualTo(errorDetail.analyticsVersion)
        assertThat(copy.code).isEqualTo(errorDetail.code)
        assertThat(copy.domain).isEqualTo(errorDetail.domain)
        assertThat(copy.errorId).isEqualTo(errorDetail.errorId)
        assertThat(copy.impressionId).isEqualTo(errorDetail.impressionId)
        assertThat(copy.licenseKey).isEqualTo(errorDetail.licenseKey)
        assertThat(copy.message).isEqualTo(longString.substring(0, 400))
        assertThat(copy.platform).isEqualTo(errorDetail.platform)
        assertThat(copy.httpRequests?.size).isEqualTo(errorDetail.httpRequests?.size)
        assertThat(copy.httpRequests?.get(0)?.url).isEqualTo("a".repeat(200))
        assertThat(copy.httpRequests?.get(0)?.lastRedirectLocation).isEqualTo("a".repeat(200))
        assertThat(copy.httpRequests?.get(1)?.url).isEqualTo(null)
        assertThat(copy.httpRequests?.get(1)?.lastRedirectLocation).isEqualTo("0123")
        assertThat(copy.data.additionalData).isEqualTo(longString.substring(0, 4000))
    }

    @Test
    fun testErrorDetailCopyTruncateStringsAndUrlsShouldCorrectlyTruncateStringsAndUrlsWithNullHttpRequests() {
        val tooLongMessage = "s".repeat(1000)

        val errorDetail = ErrorDetail("", "", "", "", 0, 0, null, tooLongMessage, ErrorData(), null)
        val copy = errorDetail.copyTruncateStringsAndUrls(2000)
        assertThat(copy.analyticsVersion).isEqualTo(errorDetail.analyticsVersion)
        assertThat(copy.code).isEqualTo(errorDetail.code)
        assertThat(copy.domain).isEqualTo(errorDetail.domain)
        assertThat(copy.errorId).isEqualTo(errorDetail.errorId)
        assertThat(copy.impressionId).isEqualTo(errorDetail.impressionId)
        assertThat(copy.licenseKey).isEqualTo(errorDetail.licenseKey)
        assertThat(copy.message).isEqualTo("s".repeat(400))
        assertThat(copy.platform).isEqualTo(errorDetail.platform)
        assertThat(copy.httpRequests?.size).isEqualTo(errorDetail.httpRequests?.size)
    }

    @Test
    fun testErrorDetailLimitHttpRequestsShouldRemoveItemsFromEnd() {
        val httpRequest1 = HttpRequest(0, HttpRequestType.MANIFEST_DASH, null, null, 0, 0L, null, 0, true)
        val httpRequest2 = HttpRequest(1, HttpRequestType.MANIFEST_DASH, null, null, 0, 0L, null, 0, true)
        val errorDetail = ErrorDetail("", "", "", "", 0, 0, null, null, ErrorData(), mutableListOf(httpRequest1, httpRequest2))
        errorDetail.copyTruncateHttpRequests(1)
        assertThat(errorDetail.httpRequests?.get(0)).isEqualTo(httpRequest1)
        assertThat(errorDetail.httpRequests?.get(0)).isNotEqualTo(httpRequest2)
    }

    @Test
    fun testWontPostWhileDisabled() {
        val backend = ErrorDetailBackend(mockk(relaxed = true), mockk(), httpClientMock)
        backend.send(getErrorDetail(0))
        verify(exactly = 0) { httpClientMock.post(any(), any(), any()) }
    }

    @Test
    fun testWillPostWhenEnabled() {
        val backend = ErrorDetailBackend(AnalyticsConfig("test", backendUrl = "http://localhost"), mockk(relaxed = true), httpClientMock)
        backend.enabled = true
        backend.send(getErrorDetail(0))
        verify(exactly = 1) { httpClientMock.post(any(), any(), any()) }
    }

    @Test
    fun testWillFlushToHttpClientIfEnabled() {
        val backend = ErrorDetailBackend(AnalyticsConfig("test", backendUrl = "http://localhost"), mockk(relaxed = true), httpClientMock)
        backend.send(getErrorDetail(0))
        backend.enabled = true
        verify(exactly = 0) { httpClientMock.post(any(), any(), any()) }
        backend.flush("test")
        verify(exactly = 1) { httpClientMock.post(any(), any(), any()) }
    }

    @Test
    fun testWillFlushToHttpClientIfEnabledAndKeyWasNotProvidedBefore() {
        val backend =
            ErrorDetailBackend(
                AnalyticsConfig("test", backendUrl = "http://localhost"),
                mockk(relaxed = true),
                httpClientMock,
            )
        backend.send(getErrorDetail(0, key = null))
        backend.enabled = true
        verify(exactly = 0) { httpClientMock.post(any(), any(), any()) }
        val licenseKey = "new-test-license-key"
        backend.flush(licenseKey)
        verify(exactly = 1) {
            httpClientMock.post(any(), match { it.contains(licenseKey) }, any())
        }
    }

    @Test
    fun testWillRequeueOnFlushIfDisabled() {
        val backend = ErrorDetailBackend(mockk(relaxed = true), mockk(relaxed = true), httpClientMock)
        backend.send(getErrorDetail(0))
        verify(exactly = 0) { httpClientMock.post(any(), any(), any()) }
        backend.flush("test")
        verify(exactly = 0) { httpClientMock.post(any(), any(), any()) }
    }

    private fun getErrorDetail(
        httpRequestCount: Int?,
        key: String? = "key",
        additionalData: String = "",
    ) = ErrorDetail(
        platform = "platform",
        licenseKey = key,
        domain = "domain",
        impressionId = "impressionId",
        errorId = 0,
        timestamp = 0,
        code = null,
        message = null,
        data = ErrorData(additionalData = additionalData),
        httpRequests =
            if (httpRequestCount == null) {
                null
            } else {
                (0..httpRequestCount).map { getHttpRequest() }
                    .toMutableList()
            },
    )

    private fun getHttpRequest() = HttpRequest(0, HttpRequestType.MANIFEST_DASH, null, null, 0, 0L, null, 0, true)
}
