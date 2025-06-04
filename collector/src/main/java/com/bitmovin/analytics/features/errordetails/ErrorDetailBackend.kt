package com.bitmovin.analytics.features.errordetails

import android.content.Context
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.dtos.ErrorDetail
import com.bitmovin.analytics.dtos.HttpRequest
import com.bitmovin.analytics.utils.DataSerializerKotlinX
import com.bitmovin.analytics.utils.HttpClient
import com.bitmovin.analytics.utils.Util
import okhttp3.OkHttpClient
import java.util.LinkedList

class ErrorDetailBackend(
    config: AnalyticsConfig,
    context: Context,
    private val httpClient: HttpClient = HttpClient(context, OkHttpClient()),
) {
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
        val errorDetailCopy =
            if (isBitmovinPlayerDecodingError(errorDetail)) {
                // we handle decoding errors differently, as they can contain a lot of additional data
                errorDetail.copyTruncateStringsAndUrls(MAX_ADDITIONAL_DATA_LENGTH_FOR_DECODING_ERRORS)
            } else {
                errorDetail.copyTruncateStringsAndUrls(MAX_ADDITIONAL_DATA_LENGTH)
            }

        if (enabled) {
            httpClient.post(backendUrl, DataSerializerKotlinX.serialize(errorDetailCopy), null)
        } else {
            _queue.add(errorDetailCopy)
        }
    }

    fun flush(licenseKey: String) {
        // We create a copy of the list to avoid side-effects like ending up in an infinite loop if we always add and remove the same element.
        // This shouldn't happen as Kotlin is call-by-value, so `send` would not modify the original queue.
        _queue
            .toList()
            .forEach {
                send(if (it.licenseKey == null) it.copy(licenseKey = licenseKey) else it)
                _queue.remove(it)
            }
    }

    fun clear() {
        _queue.clear()
    }

    companion object {
        const val MAX_URL_LENGTH = 200
        const val MAX_ERROR_MESSAGE_LENGTH = 400
        const val MAX_ADDITIONAL_DATA_LENGTH = 2000
        const val MAX_ADDITIONAL_DATA_LENGTH_FOR_DECODING_ERRORS = 4500

        fun ErrorDetail.copyTruncateStringsAndUrls(maxAdditionalDataLength: Int): ErrorDetail {
            return this.copy(
                message = message?.take(MAX_ERROR_MESSAGE_LENGTH),
                data = data.copyTruncateStrings(maxAdditionalDataLength),
                httpRequests =
                    httpRequests?.mapNotNull {
                        it.copyTruncateUrls()
                    },
            )
        }

        private fun HttpRequest?.copyTruncateUrls() =
            this?.copy(
                url = url?.take(MAX_URL_LENGTH),
                lastRedirectLocation = lastRedirectLocation?.take(MAX_URL_LENGTH),
            )

        private fun ErrorData.copyTruncateStrings(maxAdditionalDataLength: Int) =
            this.copy(
                exceptionMessage = exceptionMessage?.take(MAX_ERROR_MESSAGE_LENGTH),
                additionalData = additionalData?.take(maxAdditionalDataLength),
            )

        fun ErrorDetail.copyTruncateHttpRequests(maxRequests: Int) = this.copy(httpRequests = httpRequests?.take(maxRequests))

        fun isBitmovinPlayerDecodingError(errorDetail: ErrorDetail): Boolean {
            return errorDetail.code != null && errorDetail.code >= 2100 && errorDetail.code <= 2105
        }
    }
}
