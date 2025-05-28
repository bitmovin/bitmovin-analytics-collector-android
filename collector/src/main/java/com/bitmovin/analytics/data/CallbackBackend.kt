package com.bitmovin.analytics.data

import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.EventData

interface CallbackBackend {
    fun send(
        eventData: EventData,
        success: OnSuccessCallback? = null,
        failure: OnFailureCallback?,
    )

    fun sendAd(
        eventData: AdEventData,
        success: OnSuccessCallback? = null,
        failure: OnFailureCallback?,
    )
}

fun interface OnFailureCallback {
    fun onFailure(
        e: Exception,
        cancel: () -> Unit,
    )
}

fun interface OnSuccessCallback {
    fun onSuccess()
}
