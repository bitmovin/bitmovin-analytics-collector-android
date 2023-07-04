package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.features.httprequesttracking.HttpRequest
import com.bitmovin.analytics.utils.DataSerializer
import com.bitmovin.analytics.utils.HttpClient
import com.bitmovin.analytics.utils.Util
import okhttp3.OkHttpClient
import java.util.LinkedList

class ErrorDetailBackend(config: AnalyticsConfig, context: Context, private val httpClient: HttpClient = HttpClient(context, OkHttpClient())) {
    private val backendUrl = Util.joinUrl(config.backendUrl, "/analytics/error")
    private val _queue = LinkedList<ErrorDetail>()
    val queue: List<ErrorDetail> = _queue
    var enabled: Boolean = false

    fun limitHttpRequestsInQueue(max: Int) {
        for ((index, detail) in _queue.withIndex()) {
            _queue[index] = detail.copyTruncateHttpRequests(max)
        }
    }

    fun send(errorDetail: ErrorDetail) {
        val errorDetailCopy = errorDetail.copyTruncateStringsAndUrls(MAX_STRING_LENGTH, MAX_URL_LENGTH)
        if (enabled) {
            httpClient.post(backendUrl, DataSerializer.serialize(errorDetailCopy), null)
        } else {
            _queue.add(errorDetailCopy)
        }
    }

    fun flush() {
        // We create a copy of the list to avoid side-effects like ending up in an infinite loop if we always add and remove the same element.
        // This shouldn't happen as Kotlin is call-by-value, so `send` would not modify the original queue.
        _queue.toList().forEach {
            send(it)
            _queue.remove(it)
        }
    }

    fun clear() {
        _queue.clear()
    }

    companion object {
        const val MAX_URL_LENGTH = 200
        const val MAX_STRING_LENGTH = 400

        fun ErrorDetail.copyTruncateStringsAndUrls(maxStringLength: Int, maxUrlLength: Int): ErrorDetail = this.copy(
            message = message?.take(maxStringLength),
            data = data.copyTruncateStrings(maxStringLength),
            httpRequests = httpRequests?.mapNotNull {
                it.copyTruncateUrls(maxUrlLength)
            },
        )

        private fun HttpRequest?.copyTruncateUrls(maxLength: Int) = this?.copy(
            url = url?.take(maxLength),
            lastRedirectLocation = lastRedirectLocation?.take(maxLength),
        )

        private fun ErrorData.copyTruncateStrings(maxStringLength: Int) = this.copy(
            exceptionMessage = exceptionMessage?.take(maxStringLength),
            additionalData = additionalData?.take(maxStringLength),
        )

        fun ErrorDetail.copyTruncateHttpRequests(maxRequests: Int) = this.copy(httpRequests = httpRequests?.take(maxRequests))
    }
}
