package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.features.segmenttracking.Segment
import com.bitmovin.analytics.features.segmenttracking.SegmentType
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.network.HttpRequestType

class BitmovinSegmentTrackingAdapter(private val player: Player, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()

    private val sourceEventDownloadFinishedListener: (SourceEvent.DownloadFinished) -> Unit = { event ->
        val segmentType = mapHttpRequestType(event.downloadType)
        val segmentInfo = Segment(Util.getTimestamp(), segmentType, event.url, event.lastRedirectLocation, event.httpStatus, Util.secondsToMillis(event.downloadTime), null, event.size, event.isSuccess)
        observableSupport.notify { listener -> listener.onDownloadFinished(OnDownloadFinishedEventObject(segmentInfo)) }
    }

    init {
        wireEvents()
    }

    private fun mapHttpRequestType(requestType: HttpRequestType?): SegmentType {
        return when (requestType) {
            HttpRequestType.DrmLicenseWidevine -> SegmentType.DRM_LICENSE_WIDEVINE
            HttpRequestType.MediaThumbnails -> SegmentType.MEDIA_THUMBNAILS
            HttpRequestType.ManifestDash -> SegmentType.MANIFEST_DASH
            HttpRequestType.ManifestHlsMaster -> SegmentType.MANIFEST_HLS_MASTER
            HttpRequestType.ManifestHlsVariant -> SegmentType.MANIFEST_HLS_VARIANT
            HttpRequestType.ManifestSmooth -> SegmentType.MANIFEST_SMOOTH
            HttpRequestType.MediaVideo -> SegmentType.MEDIA_VIDEO
            HttpRequestType.MediaAudio -> SegmentType.MEDIA_AUDIO
            HttpRequestType.MediaProgressive -> SegmentType.MEDIA_PROGRESSIVE
            HttpRequestType.MediaSubtitles -> SegmentType.MEDIA_SUBTITLES
            HttpRequestType.KeyHlsAes -> SegmentType.KEY_HLS_AES
            else -> SegmentType.UNKNOWN
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
