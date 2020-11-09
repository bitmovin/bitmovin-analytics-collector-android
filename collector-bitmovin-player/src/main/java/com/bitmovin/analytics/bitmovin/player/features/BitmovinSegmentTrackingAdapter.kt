package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.features.EventEmitter
import com.bitmovin.analytics.features.EventSource
import com.bitmovin.analytics.features.segmenttracking.DownloadFinishedEvent
import com.bitmovin.analytics.features.segmenttracking.Segment
import com.bitmovin.analytics.features.segmenttracking.DownloadFinishedEventListener
import com.bitmovin.analytics.features.segmenttracking.SegmentType
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnDownloadFinishedListener
import com.bitmovin.player.config.network.HttpRequestType

class BitmovinSegmentTrackingAdapter(private val player: BitmovinPlayer) : EventSource<DownloadFinishedEventListener> {
    private val eventEmitter: EventEmitter = EventEmitter()
    private val onDownloadFinishedListener = OnDownloadFinishedListener {
        val segmentType = mapHttpRequestType(it.downloadType)
        val segmentInfo = Segment(it.timestamp, segmentType, it.url, it.lastRedirectLocation, it.httpStatus, it.downloadTime, it.size, it.isSuccess)
        eventEmitter.emit(DownloadFinishedEventListener::class) { listener -> listener.onDownloadFinished(DownloadFinishedEvent(segmentInfo)) }
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
        player.addEventListener(onDownloadFinishedListener)
    }

    fun unwireEvents() {
        player.removeEventListener(onDownloadFinishedListener)
    }

    override fun addEventListener(listener: DownloadFinishedEventListener) {
        eventEmitter.addEventListener(listener)
    }

    override fun removeEventListener(listener: DownloadFinishedEventListener) {
        eventEmitter.removeEventListener(listener)
    }
}
