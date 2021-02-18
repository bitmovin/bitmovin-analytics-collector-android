package com.bitmovin.analytics.features.segmenttracking

import com.bitmovin.analytics.EventListener

interface Observable<TListener> {
    fun subscribe(listener: TListener)
    fun unsubscribe(listener: TListener)
}

interface OnDownloadFinishedEventSource: Observable<OnDownloadFinishedEventListener>

interface OnDownloadFinishedEventListener : EventListener {
    fun onDownloadFinished(event: OnDownloadFinishedEventObject)
}
