package com.bitmovin.analytics.features.httprequesttracking

interface OnDownloadFinishedEventListener {
    fun onDownloadFinished(event: OnDownloadFinishedEventObject)
}
