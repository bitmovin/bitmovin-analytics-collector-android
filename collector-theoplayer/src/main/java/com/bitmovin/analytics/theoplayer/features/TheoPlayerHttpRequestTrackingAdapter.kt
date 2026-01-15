package com.bitmovin.analytics.theoplayer.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.dtos.HttpRequest
import com.bitmovin.analytics.dtos.HttpRequestType
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.network.http.HTTPInterceptor
import com.theoplayer.android.api.network.http.InterceptableHTTPResponse
import com.theoplayer.android.api.player.Player

internal class TheoPlayerHttpRequestTrackingAdapter(
    private val player: Player,
    private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>,
) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()
    private val interceptor = TheoPlayerNetworkRequestInterceptor(observableSupport)

    init {
        wireEvents()
    }

//    private fun mapHttpRequestType(requestType: HttpRequestType?): HttpRequestType {
//        return when (requestType) {
//            HttpRequestType.DrmLicenseWidevine -> HttpRequestType.DRM_LICENSE_WIDEVINE
//            HttpRequestType.MediaThumbnails -> HttpRequestType.MEDIA_THUMBNAILS
//            HttpRequestType.ManifestDash -> HttpRequestType.MANIFEST_DASH
//            HttpRequestType.ManifestHlsMaster -> HttpRequestType.MANIFEST_HLS_MASTER
//            HttpRequestType.ManifestHlsVariant -> HttpRequestType.MANIFEST_HLS_VARIANT
//            HttpRequestType.ManifestSmooth -> HttpRequestType.MANIFEST_SMOOTH
//            HttpRequestType.MediaVideo -> HttpRequestType.MEDIA_VIDEO
//            HttpRequestType.MediaAudio -> HttpRequestType.MEDIA_AUDIO
//            HttpRequestType.MediaProgressive -> HttpRequestType.MEDIA_PROGRESSIVE
//            HttpRequestType.MediaSubtitles -> HttpRequestType.MEDIA_SUBTITLES
//            HttpRequestType.KeyHlsAes -> HttpRequestType.KEY_HLS_AES
//            else -> HttpRequestType.UNKNOWN
//        }
//    }

    private fun wireEvents() {
        // TODO: get rid of this circular dependency, not good style
        onAnalyticsReleasingObservable.subscribe(this)
        player.network.addHTTPInterceptor(interceptor)
    }

    private fun unwireEvents() {
        onAnalyticsReleasingObservable.unsubscribe(this)
        player.network.removeHTTPInterceptor(interceptor)
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

    companion object {
        private val TAG = TheoPlayerHttpRequestTrackingAdapter::class.java.name
    }
}

internal class TheoPlayerNetworkRequestInterceptor(
    private val observableSupport: ObservableSupport<OnDownloadFinishedEventListener>,
) : HTTPInterceptor {
    override suspend fun onResponse(response: InterceptableHTTPResponse) {
        catchAndLogException("Exception occurred in SourceEvent.DownloadFinished") {
            val url = response.url.path
            val httpStatus = response.status
            // TODO: can we get that one?
            val lastRedirectLocation = null
            // TODO: does this work?, do we need to use content size from the header?
            val sizeInBytes = response.request.body?.size?.toLong()
            val isSuccess = true
            val downloadTime = 0L // TODO: is this possible?

            val httpRequest =
                HttpRequest(
                    Util.timestamp,
                    HttpRequestType.UNKNOWN.value,
                    url,
                    lastRedirectLocation,
                    httpStatus,
                    downloadTime,
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

        private val TAG = TheoPlayerNetworkRequestInterceptor::class.java.name
    }
}
