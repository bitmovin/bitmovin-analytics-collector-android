package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.features.httprequesttracking.HttpRequest
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.SourceEvent

class BitmovinHttpRequestTrackingAdapter(private val player: Player, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()

    private val sourceEventDownloadFinishedListener: (SourceEvent.DownloadFinished) -> Unit = { event ->
        val segmentType = mapHttpRequestType(event.downloadType)
        val segmentInfo = HttpRequest(Util.getTimestamp(), segmentType, event.url, event.lastRedirectLocation, event.httpStatus, Util.secondsToMillis(event.downloadTime), null, event.size, event.isSuccess)
        observableSupport.notify { listener -> listener.onDownloadFinished(OnDownloadFinishedEventObject(segmentInfo)) }
    }

    init {
        wireEvents()
    }

    private fun mapHttpRequestType(requestType: HttpRequestType?): com.bitmovin.analytics.features.httprequesttracking.HttpRequestType {
        return when (requestType) {
            HttpRequestType.DrmLicenseWidevine -> HttpRequestType.DRM_LICENSE_WIDEVINE
            HttpRequestType.MediaThumbnails -> HttpRequestType.MEDIA_THUMBNAILS
            HttpRequestType.ManifestDash -> HttpRequestType.MANIFEST_DASH
            HttpRequestType.ManifestHlsMaster -> HttpRequestType.MANIFEST_HLS_MASTER
            HttpRequestType.ManifestHlsVariant -> HttpRequestType.MANIFEST_HLS_VARIANT
            HttpRequestType.ManifestSmooth -> HttpRequestType.MANIFEST_SMOOTH
            HttpRequestType.MediaVideo -> HttpRequestType.MEDIA_VIDEO
            HttpRequestType.MediaAudio -> HttpRequestType.MEDIA_AUDIO
            HttpRequestType.MediaProgressive -> HttpRequestType.MEDIA_PROGRESSIVE
            HttpRequestType.MediaSubtitles -> HttpRequestType.MEDIA_SUBTITLES
            HttpRequestType.KeyHlsAes -> HttpRequestType.KEY_HLS_AES
            else -> HttpRequestType.UNKNOWN
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
