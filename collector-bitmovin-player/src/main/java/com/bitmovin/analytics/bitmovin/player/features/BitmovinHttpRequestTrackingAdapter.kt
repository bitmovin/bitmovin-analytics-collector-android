package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.dtos.HttpRequest
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.httprequesttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.network.HttpRequestType

internal class BitmovinHttpRequestTrackingAdapter(
    private val player: Player,
    private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>,
) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()

    private val sourceEventDownloadFinishedListener: (SourceEvent.DownloadFinished) -> Unit = { event ->
        catchAndLogException("Exception occurred in SourceEvent.DownloadFinished") {
            val requestType = mapHttpRequestType(event.downloadType)
            val httpRequest =
                HttpRequest(
                    Util.timestamp, requestType.value, event.url, event.lastRedirectLocation, event.httpStatus,
                    Util.secondsToMillis(
                        event.downloadTime,
                    ),
                    null,
                    event.size,
                    event.isSuccess,
                )
            observableSupport.notify { listener -> listener.onDownloadFinished(OnDownloadFinishedEventObject(httpRequest)) }
        }
    }

    init {
        wireEvents()
    }

    private fun mapHttpRequestType(requestType: HttpRequestType?): com.bitmovin.analytics.dtos.HttpRequestType {
        return when (requestType) {
            HttpRequestType.DrmLicenseWidevine -> com.bitmovin.analytics.dtos.HttpRequestType.DRM_LICENSE_WIDEVINE
            HttpRequestType.MediaThumbnails -> com.bitmovin.analytics.dtos.HttpRequestType.MEDIA_THUMBNAILS
            HttpRequestType.ManifestDash -> com.bitmovin.analytics.dtos.HttpRequestType.MANIFEST_DASH
            HttpRequestType.ManifestHlsMaster -> com.bitmovin.analytics.dtos.HttpRequestType.MANIFEST_HLS_MASTER
            HttpRequestType.ManifestHlsVariant -> com.bitmovin.analytics.dtos.HttpRequestType.MANIFEST_HLS_VARIANT
            HttpRequestType.ManifestSmooth -> com.bitmovin.analytics.dtos.HttpRequestType.MANIFEST_SMOOTH
            HttpRequestType.MediaVideo -> com.bitmovin.analytics.dtos.HttpRequestType.MEDIA_VIDEO
            HttpRequestType.MediaAudio -> com.bitmovin.analytics.dtos.HttpRequestType.MEDIA_AUDIO
            HttpRequestType.MediaProgressive -> com.bitmovin.analytics.dtos.HttpRequestType.MEDIA_PROGRESSIVE
            HttpRequestType.MediaSubtitles -> com.bitmovin.analytics.dtos.HttpRequestType.MEDIA_SUBTITLES
            HttpRequestType.KeyHlsAes -> com.bitmovin.analytics.dtos.HttpRequestType.KEY_HLS_AES
            else -> com.bitmovin.analytics.dtos.HttpRequestType.UNKNOWN
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

    companion object {
        private val TAG = BitmovinHttpRequestTrackingAdapter::class.java.name

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
    }
}
