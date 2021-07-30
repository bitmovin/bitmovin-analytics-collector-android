package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.httprequesttracking.HttpRequest
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.network.HttpRequestType

class BitmovinHttpRequestTrackingAdapter(private val player: Player, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()

    private val sourceEventDownloadFinishedListener: (SourceEvent.DownloadFinished) -> Unit = { event ->
        val requestType = mapHttpRequestType(event.downloadType)
        val httpRequest = HttpRequest(Util.getTimestamp(), requestType, event.url, event.lastRedirectLocation, event.httpStatus, Util.secondsToMillis(event.downloadTime), null, event.size, event.isSuccess)
        observableSupport.notify { listener -> listener.onDownloadFinished(OnDownloadFinishedEventObject(httpRequest)) }
    }

    init {
        wireEvents()
    }

    private fun mapHttpRequestType(requestType: HttpRequestType?): com.bitmovin.analytics.features.httprequesttracking.HttpRequestType {
        return when (requestType) {
            HttpRequestType.DrmLicenseWidevine -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.DRM_LICENSE_WIDEVINE
            HttpRequestType.MediaThumbnails -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MEDIA_THUMBNAILS
            HttpRequestType.ManifestDash -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MANIFEST_DASH
            HttpRequestType.ManifestHlsMaster -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MANIFEST_HLS_MASTER
            HttpRequestType.ManifestHlsVariant -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MANIFEST_HLS_VARIANT
            HttpRequestType.ManifestSmooth -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MANIFEST_SMOOTH
            HttpRequestType.MediaVideo -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MEDIA_VIDEO
            HttpRequestType.MediaAudio -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MEDIA_AUDIO
            HttpRequestType.MediaProgressive -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MEDIA_PROGRESSIVE
            HttpRequestType.MediaSubtitles -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.MEDIA_SUBTITLES
            HttpRequestType.KeyHlsAes -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.KEY_HLS_AES
            else -> com.bitmovin.analytics.features.httprequesttracking.HttpRequestType.UNKNOWN
        }
    }

    private fun wireEvents() {
        onAnalyticsReleasingObservable.subscribe(this)
        player.on(SourceEvent.DownloadFinished::class, sourceEventDownloadFinishedListener)
    }

    private fun unwireEvents() {
        onAnalyticsReleasingObservable.unsubscribe(this)
        player.off(sourceEventDownloadFinishedListener)
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
