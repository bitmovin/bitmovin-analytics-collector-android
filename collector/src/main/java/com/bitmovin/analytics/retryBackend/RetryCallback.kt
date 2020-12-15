package com.bitmovin.analytics.retryBackend

import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

interface RetryCallback : Callback {
    override fun onFailure(call: Call, e: IOException) {
    }

    override fun onResponse(call: Call, response: Response) {
    }
}
