package com.bitmovin.analytics.features.segmenttracking

interface DownloadFinishedEventListener {
    fun onDownloadFinished(event: DownloadFinishedEvent)
}
