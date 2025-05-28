package com.bitmovin.analytics.features.httprequesttracking

import com.bitmovin.analytics.dtos.HttpRequest

data class OnDownloadFinishedEventObject(val httpRequest: HttpRequest)
