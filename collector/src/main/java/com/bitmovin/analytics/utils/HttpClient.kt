package com.bitmovin.analytics.utils

import android.content.Context
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class HttpClient(private val context: Context, private val client: OkHttpClient) {
    fun post(
        url: String,
        postBody: String?,
        callback: Callback?,
        additionalHeaders: Headers? = null,
    ) {
        Log.d(TAG, String.format("Posting Analytics JSON: \n%s\n", postBody))

        val requestBuilder =
            Request.Builder()
                .url(url)
                .header("Origin", String.format("http://%s", context.packageName))
                .post(postBody.orEmpty().toRequestBody(JSON_CONTENT_TYPE))

        additionalHeaders?.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val request = requestBuilder.build()

        client.newCall(request)
            .enqueue(
                object : Callback {
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        Log.e(TAG, "HTTP Error: ", e)
                        callback?.onFailure(call, e)
                    }

                    @Throws(IOException::class)
                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        callback?.onResponse(call, response)
                        response.close()
                    }
                },
            )
    }

    companion object {
        private val JSON_CONTENT_TYPE: MediaType = "application/json; charset=utf-8".toMediaType()
        private const val TAG = "HttpClient"
    }
}
