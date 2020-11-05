package com.bitmovin.analytics.features.segmenttracking

interface SegmentTrackingEventListener {
    fun onDownloadFinished(event: DownloadFinishedEvent)
}
