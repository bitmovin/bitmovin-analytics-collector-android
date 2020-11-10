package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.OnAnalyticsReleasingEventSource
import com.bitmovin.analytics.features.EventEmitter
import com.bitmovin.analytics.features.segmenttracking.DownloadFinishedEvent
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventListener
import com.bitmovin.analytics.features.segmenttracking.OnDownloadFinishedEventSource
import com.bitmovin.analytics.features.segmenttracking.Segment
import com.bitmovin.analytics.features.segmenttracking.SegmentType
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnDownloadFinishedListener
import com.bitmovin.player.config.network.HttpRequestType

class BitmovinSegmentTrackingAdapter(private val player: BitmovinPlayer, private val onAnalyticsReleasingEventSource: OnAnalyticsReleasingEventSource) : OnDownloadFinishedEventSource, OnAnalyticsReleasingEventListener {
    private val eventEmitter: EventEmitter = EventEmitter()
    private val onDownloadFinishedListener = OnDownloadFinishedListener {
        val segmentType = mapHttpRequestType(it.downloadType)
        val segmentInfo = Segment(it.timestamp, segmentType, it.url, it.lastRedirectLocation, it.httpStatus, it.downloadTime, it.size, it.isSuccess)
        eventEmitter.emit(OnDownloadFinishedEventListener::class) { listener -> listener.onDownloadFinished(DownloadFinishedEvent(segmentInfo)) }
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
            else -> SegmentType.UNKNOWN
        }
    }

    private fun wireEvents() {
        onAnalyticsReleasingEventSource.addEventListener(this)
        player.addEventListener(onDownloadFinishedListener)
    }

    fun unwireEvents() {
        onAnalyticsReleasingEventSource.removeEventListener(this)
        player.removeEventListener(onDownloadFinishedListener)
    }

    override fun addEventListener(listener: OnDownloadFinishedEventListener) {
        eventEmitter.addEventListener(listener)
    }

    override fun removeEventListener(listener: OnDownloadFinishedEventListener) {
        eventEmitter.removeEventListener(listener)
    }

    override fun onReleasing() {
        unwireEvents()
    }
}
