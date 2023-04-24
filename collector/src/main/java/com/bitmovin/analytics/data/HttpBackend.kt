package com.bitmovin.analytics.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bitmovin.analytics.CollectorConfig
import com.bitmovin.analytics.utils.ClientFactory
import com.bitmovin.analytics.utils.DataSerializer.serialize
import com.bitmovin.analytics.utils.HttpClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class HttpBackend(config: CollectorConfig, context: Context) : Backend, CallbackBackend {
    private val httpClient: HttpClient
    private val analyticsBackendUrl: String
    private val adsAnalyticsBackendUrl: String

    init {
        analyticsBackendUrl = Uri.parse(config.backendUrl)
            .buildUpon()
            .appendEncodedPath("analytics")
            .build()
            .toString()
        adsAnalyticsBackendUrl = Uri.parse(config.backendUrl)
            .buildUpon()
            .appendEncodedPath("analytics/a")
            .build()
            .toString()
        Log.d(
            TAG,
            String.format("Initialized Analytics HTTP Backend with %s", analyticsBackendUrl),
        )
        httpClient = HttpClient(context, ClientFactory().createClient(config))
    }

    override fun send(eventData: EventData) {
        this.send(eventData, null, null)
    }

    override fun sendAd(eventData: AdEventData) {
        this.sendAd(eventData, null, null)
    }

    override fun send(eventData: EventData, success: OnSuccessCallback?, failure: OnFailureCallback?) {
        Log.d(
            TAG,
            String.format(
                "Sending sample: %s (state: %s, videoId: %s, startupTime: %d, videoStartupTime: %d, buffered: %d, audioLanguage: %s)",
                eventData.impressionId,
                eventData.state,
                eventData.videoId,
                eventData.startupTime,
                eventData.videoStartupTime,
                eventData.buffered,
                eventData.audioLanguage,
            ),
        )
        httpClient.post(
            analyticsBackendUrl,
            serialize(eventData),
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    failure?.onFailure(e) { call.cancel() }
                }

                override fun onResponse(call: Call, response: Response) {
                    success?.onSuccess()
                }
            },
        )
    }

    override fun sendAd(eventData: AdEventData, success: OnSuccessCallback?, failure: OnFailureCallback?) {
        Log.d(
            TAG,
            String.format(
                "Sending ad sample: %s (videoImpressionId: %s, adImpressionId: %s)",
                eventData.adImpressionId,
                eventData.videoImpressionId,
                eventData.adImpressionId,
            ),
        )
        httpClient.post(
            adsAnalyticsBackendUrl,
            serialize(eventData),
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    failure?.onFailure(e) { call.cancel() }
                }

                override fun onResponse(call: Call, response: Response) {
                    success?.onSuccess()
                }
            },
        )
    }

    companion object {
        private const val TAG = "BitmovinBackend"
    }
}
