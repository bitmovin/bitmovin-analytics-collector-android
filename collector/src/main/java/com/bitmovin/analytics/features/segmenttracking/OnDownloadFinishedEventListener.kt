package com.bitmovin.analytics.features.segmenttracking

import com.bitmovin.analytics.EventListener

interface OnDownloadFinishedEventSource {
    fun addEventListener(listener: OnDownloadFinishedEventListener)
    fun removeEventListener(listener: OnDownloadFinishedEventListener)
}

interface OnDownloadFinishedEventListener : EventListener {
    fun onDownloadFinished(event: DownloadFinishedEvent)
}
