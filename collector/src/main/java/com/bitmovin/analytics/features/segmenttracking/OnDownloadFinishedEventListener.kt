package com.bitmovin.analytics.features.segmenttracking

import com.bitmovin.analytics.EventListener
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener

interface OnDownloadFinishedEventSource {
    fun addEventListener(listener: OnDownloadFinishedEventListener)
    fun removeEventListener(listener: OnDownloadFinishedEventListener)
}

interface OnDownloadFinishedEventListener : EventListener {
    fun onDownloadFinished(event: DownloadFinishedEvent)
}
