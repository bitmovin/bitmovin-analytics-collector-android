package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventObject
import com.bitmovin.analytics.features.segmenttracking.Segment
import com.bitmovin.analytics.features.segmenttracking.SegmentType
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnDownloadFinishedListener
import com.bitmovin.player.config.network.HttpRequestType

class BitmovinSegmentTrackingAdapter(private val player: BitmovinPlayer, private val onAnalyticsReleasingObservable: Observable<OnAnalyticsReleasingEventListener>) : Observable<OnDownloadFinishedEventListener>, OnAnalyticsReleasingEventListener {
    private val observableSupport = ObservableSupport<OnDownloadFinishedEventListener>()
    private val onDownloadFinishedListener = OnDownloadFinishedListener {
        val segmentType = mapHttpRequestType(it.downloadType)
        val segmentInfo = Segment(Util.getTimestamp(), segmentType, it.url, it.lastRedirectLocation, it.httpStatus, Util.secondsToMillis(it.downloadTime), null, it.size, it.isSuccess)
        observableSupport.notify { listener -> listener.onDownloadFinished(OnDownloadFinishedEventObject(segmentInfo)) }
    }

    init {
        wireEvents()
    }

    private fun mapHttpRequestType(requestType: HttpRequestType?): SegmentType {
        return when (requestType) {
            HttpRequestType.DRM_LICENSE_WIDEVINE -> SegmentType.DRM_LICENSE_WIDEVINE
            HttpRequestType.MEDIA_THUMBNAILS -> SegmentType.MEDIA_THUMBNAILS
            HttpRequestType.MANIFEST_DASH -> SegmentType.MANIFEST_DASH
            HttpRequestType.MANIFEST_HLS_MASTER -> SegmentType.MANIFEST_HLS_MASTER
            HttpRequestType.MANIFEST_HLS_VARIANT -> SegmentType.MANIFEST_HLS_VARIANT
            HttpRequestType.MANIFEST_SMOOTH -> SegmentType.MANIFEST_SMOOTH
            HttpRequestType.KEY_HLS_AES -> SegmentType.KEY_HLS_AES
            HttpRequestType.MEDIA_AUDIO -> SegmentType.MEDIA_AUDIO
            HttpRequestType.MEDIA_SUBTITLES -> SegmentType.MEDIA_SUBTITLES
            HttpRequestType.MEDIA_VIDEO -> SegmentType.MEDIA_VIDEO
            else -> SegmentType.UNKNOWN
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
