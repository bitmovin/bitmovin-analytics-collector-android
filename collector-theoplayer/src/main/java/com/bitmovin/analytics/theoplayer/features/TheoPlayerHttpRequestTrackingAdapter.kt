package com.bitmovin.analytics.theoplayer.features

import android.util.LruCache
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.dtos.HttpRequest
import com.bitmovin.analytics.dtos.HttpRequestType
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.theoplayer.player.DrmInfoProvider
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.network.http.HTTPInterceptor
import com.theoplayer.android.api.network.http.InterceptableHTTPRequest
import com.theoplayer.android.api.network.http.InterceptableHTTPResponse
import com.theoplayer.android.api.network.http.RequestMediaType
import com.theoplayer.android.api.network.http.RequestSubType
import com.theoplayer.android.api.network.http.RequestType
import com.theoplayer.android.api.player.Player

internal class TheoPlayerHttpRequestTrackingAdapter(
    private val player: Player,
    private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>,
    drmInfoProvider: DrmInfoProvider,
) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()
    private val interceptor = TheoPlayerNetworkRequestInterceptor(drmInfoProvider, observableSupport)

    init {
        wireEvents()
    }

    private fun wireEvents() {
        // TODO: get rid of this circular dependency, not good style
        onAnalyticsReleasingObservable.subscribe(this)
        player.network.addHTTPInterceptor(interceptor)
    }

    private fun unwireEvents() {
        onAnalyticsReleasingObservable.unsubscribe(this)
        player.network.removeHTTPInterceptor(interceptor)
        interceptor.reset()
    }

    override fun subscribe(listener: OnDownloadFinishedEventListener) {
        observableSupport.subscribe(listener)
    }

    override fun unsubscribe(listener: OnDownloadFinishedEventListener) {
        observableSupport.unsubscribe(listener)
    }

    override fun onReleasing() {
        unwireEvents()
    }
}

internal class TheoPlayerNetworkRequestInterceptor(
    private val drmInfoProvider: DrmInfoProvider,
    private val observableSupport: ObservableSupport<OnDownloadFinishedEventListener>,
) : HTTPInterceptor {
    // Urls can practically be up to 2000 characters, this means we could
    // potentially have 100*2000*4bytes (utf-8) = 800kb cache
    private val requestStartTimes = LruCache<String, Long>(MAX_CACHE_SIZE)

    fun reset() {
        requestStartTimes.evictAll()
        drmInfoProvider.reset()
    }

    override suspend fun onRequest(request: InterceptableHTTPRequest) {
        val trimmedUrl = truncateStartOfString(request.url.toString())
        requestStartTimes.put(trimmedUrl, Util.elapsedTime)
    }

    override suspend fun onResponse(response: InterceptableHTTPResponse) {
        catchAndLogException("Exception occurred in TheoPlayer HTTP response interceptor") {
            val url = response.url.toString()
            val trimmedUrl = truncateStartOfString(url)
            val requestStartTime: Long? = requestStartTimes.remove(trimmedUrl)
            val downloadTimeInMs = if (requestStartTime != null) Util.elapsedTime - requestStartTime else 0L
            val sizeInBytes = extractContentLengthFromHeaders(response.headers)
            val httpStatus = response.status
            val isSuccess = httpStatus in 200..399
            val requestType = mapHttpRequestType(response.request)

            // in case we detect a DRM request, we set the request time
            // this is only done for licensing requests to be consistent
            // we ignore the provisioning calls
            if (isDrmLicensingRequest(response.request.subType) && downloadTimeInMs != 0L) {
                drmInfoProvider.setDrmLicenseRequestTimeInMs(downloadTimeInMs)
            }

            val httpRequest =
                HttpRequest(
                    Util.timestamp,
                    requestType.value,
                    url,
                    null,
                    httpStatus,
                    downloadTimeInMs,
                    null,
                    sizeInBytes,
                    isSuccess,
                )
            observableSupport.notify { listener ->
                listener.onDownloadFinished(OnDownloadFinishedEventObject(httpRequest))
            }
        }
    }

    companion object {
        private val TAG = TheoPlayerNetworkRequestInterceptor::class.java.name
        private const val MAX_URL_LENGTH = 2000
        private const val MAX_CACHE_SIZE = 100

        // truncate string if it exceeds limit
        private fun truncateStartOfString(stringToLimit: String): String {
            if (stringToLimit.length <= MAX_URL_LENGTH) {
                return stringToLimit
            }

            return stringToLimit.takeLast(MAX_URL_LENGTH)
        }

        private fun extractContentLengthFromHeaders(headers: MutableMap<String, String>): Long? {
            val sizeInBytes =
                headers.entries
                    .firstOrNull { it.key.equals("content-length", ignoreCase = true) }
                    ?.value?.toLongOrNull()
            return sizeInBytes
        }

        private fun catchAndLogException(
            msg: String,
            block: () -> Unit,
        ) {
            try {
                block()
            } catch (e: Exception) {
                BitmovinLog.e(TAG, msg, e)
            }
        }

        private fun mapHttpRequestType(request: com.theoplayer.android.api.network.http.HTTPRequest): HttpRequestType {
            return when (request.type) {
                RequestType.CONTENT_PROTECTION -> mapContentProtectionType(request.subType)
                RequestType.MANIFEST -> HttpRequestType.MANIFEST
                RequestType.SEGMENT -> mapSegmentType(request.mediaType)
                else -> HttpRequestType.UNKNOWN
            }
        }

        private fun mapContentProtectionType(subType: RequestSubType): HttpRequestType {
            return when (subType) {
                RequestSubType.WIDEVINE_LICENSE, RequestSubType.WIDEVINE_CERTIFICATE -> HttpRequestType.DRM_LICENSE_WIDEVINE
                RequestSubType.AES128_KEY -> HttpRequestType.KEY_HLS_AES
                RequestSubType.FAIRPLAY_LICENSE, RequestSubType.FAIRPLAY_CERTIFICATE,
                RequestSubType.PLAYREADY_LICENSE, RequestSubType.CLEARKEY_LICENSE,
                -> HttpRequestType.DRM_OTHER

                else -> HttpRequestType.DRM_OTHER
            }
        }

        private fun mapSegmentType(mediaType: RequestMediaType): HttpRequestType {
            return when (mediaType) {
                RequestMediaType.AUDIO -> HttpRequestType.MEDIA_AUDIO
                RequestMediaType.VIDEO -> HttpRequestType.MEDIA_VIDEO
                RequestMediaType.TEXT -> HttpRequestType.MEDIA_SUBTITLES
                RequestMediaType.IMAGE -> HttpRequestType.MEDIA_THUMBNAILS
                else -> HttpRequestType.UNKNOWN
            }
        }

        private fun isDrmLicensingRequest(subType: RequestSubType): Boolean {
            return subType == RequestSubType.WIDEVINE_LICENSE ||
                subType == RequestSubType.CLEARKEY_LICENSE ||
                subType == RequestSubType.FAIRPLAY_LICENSE ||
                subType == RequestSubType.PLAYREADY_LICENSE
        }
    }
}
