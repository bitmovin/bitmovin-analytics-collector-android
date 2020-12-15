package com.bitmovin.analytics.data

import android.util.Log
import com.bitmovin.analytics.retryBackend.RetryQueue
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.internal.http2.StreamResetException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface Backend {
    fun send(eventData: EventData)
    fun sendAd(eventData: AdEventData)
}

interface CallbackBackend {
    fun send(eventData: EventData, callback: Callback?)
    fun sendAd(eventData: AdEventData, callback: Callback?)
}

