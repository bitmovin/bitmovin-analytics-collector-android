package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.features.httprequesttracking.HttpRequest
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnDownloadFinishedListener

class BitmovinSegmentTrackingAdapter(private val player: BitmovinPlayer, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()
    private val onDownloadFinishedListener = OnDownloadFinishedListener {
        val segmentType = mapHttpRequestType(it.downloadType)
        val segmentInfo = HttpRequest(Util.getTimestamp(), segmentType, it.url, it.lastRedirectLocation, it.httpStatus, Util.secondsToMillis(it.downloadTime), null, it.size, it.isSuccess)
        observableSupport.notify { listener -> listener.onDownloadFinished(OnDownloadFinishedEventObject(segmentInfo)) }
    }

    init {
        wireEvents()
    }

    private fun mapHttpRequestType(requestType: HttpRequestType?): com.bitmovin.analytics.features.httprequesttracking.HttpRequestType {
        return when (requestType) {
            HttpRequestType.DRM_LICENSE_WIDEVINE -> HttpRequestType.DRM_LICENSE_WIDEVINE
            HttpRequestType.MEDIA_THUMBNAILS -> HttpRequestType.MEDIA_THUMBNAILS
            HttpRequestType.MANIFEST_DASH -> HttpRequestType.MANIFEST_DASH
            HttpRequestType.MANIFEST_HLS_MASTER -> HttpRequestType.MANIFEST_HLS_MASTER
            HttpRequestType.MANIFEST_HLS_VARIANT -> HttpRequestType.MANIFEST_HLS_VARIANT
            HttpRequestType.MANIFEST_SMOOTH -> HttpRequestType.MANIFEST_SMOOTH
            HttpRequestType.KEY_HLS_AES -> HttpRequestType.KEY_HLS_AES
            HttpRequestType.MEDIA_AUDIO -> HttpRequestType.MEDIA_AUDIO
            HttpRequestType.MEDIA_SUBTITLES -> HttpRequestType.MEDIA_SUBTITLES
            HttpRequestType.MEDIA_VIDEO -> HttpRequestType.MEDIA_VIDEO
            else -> HttpRequestType.UNKNOWN
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
}
