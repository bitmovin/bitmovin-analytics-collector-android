package com.bitmovin.analytics.bitmovin.player.features

import android.util.Log
import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.httprequesttracking.HttpRequest
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnDownloadFinishedListener
import com.bitmovin.player.config.network.HttpRequestType

class BitmovinHttpRequestTrackingAdapter(private val player: BitmovinPlayer, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()
    private val onDownloadFinishedListener = OnDownloadFinishedListener {
        catchAndLogException("Exception occurred in OnDownloadFinishedListener") {
            val requestType = mapHttpRequestType(it.downloadType)
            val httpRequest = HttpRequest(Util.getTimestamp(), requestType, it.url, it.lastRedirectLocation, it.httpStatus, Util.secondsToMillis(it.downloadTime), null, it.size, it.isSuccess)
            observableSupport.notify { listener -> listener.onDownloadFinished(OnDownloadFinishedEventObject(httpRequest)) }
        }
    }

    init {
        wireEvents()
    }

    private fun mapHttpRequestType(requestType: HttpRequestType?): com.bitmovin.analytics.features.httprequesttracking.HttpRequestType {
        return when (requestType) {
            HttpRequestType.DRM_LICENSE_WIDEVINE -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.DRM_LICENSE_WIDEVINE
            HttpRequestType.MEDIA_THUMBNAILS -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MEDIA_THUMBNAILS
            HttpRequestType.MANIFEST_DASH -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MANIFEST_DASH
            HttpRequestType.MANIFEST_HLS_MASTER -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MANIFEST_HLS_MASTER
            HttpRequestType.MANIFEST_HLS_VARIANT -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MANIFEST_HLS_VARIANT
            HttpRequestType.MANIFEST_SMOOTH -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MANIFEST_SMOOTH
            HttpRequestType.KEY_HLS_AES -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.KEY_HLS_AES
            HttpRequestType.MEDIA_AUDIO -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MEDIA_AUDIO
            HttpRequestType.MEDIA_SUBTITLES -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MEDIA_SUBTITLES
            HttpRequestType.MEDIA_VIDEO -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MEDIA_VIDEO
            else -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.UNKNOWN
        }
    }

    private fun wireEvents() {
        onAnalyticsReleasingObservable.subscribe(this)
        player.addEventListener(onDownloadFinishedListener)
    }

    private fun unwireEvents() {
        onAnalyticsReleasingObservable.unsubscribe(this)
        player.removeEventListener(onDownloadFinishedListener)
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
        private val TAG = BitmovinHttpRequestTrackingAdapter::class.java.name

        private fun catchAndLogException(msg: String, block: () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, msg, e)
            }
        }
    }
}
