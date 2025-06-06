package com.bitmovin.analytics.data

import android.content.Context
import android.net.Uri
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.ClientFactory
import com.bitmovin.analytics.utils.DataSerializerKotlinX.serialize
import com.bitmovin.analytics.utils.HttpClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.util.Locale

class HttpBackend(config: AnalyticsConfig, context: Context) : Backend, CallbackBackend {
    private val httpClient: HttpClient
    private val analyticsBackendUrl: String
    private val adsAnalyticsBackendUrl: String

    init {
        analyticsBackendUrl =
            Uri.parse(config.backendUrl)
                .buildUpon()
                .appendEncodedPath("analytics")
                .build()
                .toString()
        adsAnalyticsBackendUrl =
            Uri.parse(config.backendUrl)
                .buildUpon()
                .appendEncodedPath("analytics/a")
                .build()
                .toString()

        BitmovinLog.d(
            TAG,
            String.format("Initialized Analytics HTTP Backend with %s", analyticsBackendUrl),
        )

        httpClient = HttpClient(context, ClientFactory().createClient(config))
    }

    override fun send(eventData: EventData) = send(eventData, null, null)

    override fun sendAd(eventData: AdEventData) = sendAd(eventData, null, null)

    override fun send(
        eventData: EventData,
        success: OnSuccessCallback?,
        failure: OnFailureCallback?,
    ) {
        BitmovinLog.d(
            TAG,
            String.format(
                Locale.US,
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
                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) {
                    failure?.onFailure(e) { call.cancel() }
                }

                override fun onResponse(
                    call: Call,
                    response: Response,
                ) {
                    success?.onSuccess()
                }
            },
            eventData.ssaiRelatedSample,
        )
    }

    override fun sendAd(
        eventData: AdEventData,
        success: OnSuccessCallback?,
        failure: OnFailureCallback?,
    ) {
        BitmovinLog.d(
            TAG,
            String.format(
                "Sending ad sample: %s (videoImpressionId: %s, adImpressionId: %s)",
                eventData.adImpressionId,
                eventData.videoImpressionId,
                eventData.adImpressionId,
            ),
        )

        val useSsaiRouting = eventData.adType == AdType.SERVER_SIDE.value

        httpClient.post(
            adsAnalyticsBackendUrl,
            serialize(eventData),
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException,
                ) {
                    failure?.onFailure(e) { call.cancel() }
                }

                override fun onResponse(
                    call: Call,
                    response: Response,
                ) {
                    success?.onSuccess()
                }
            },
            useSsaiRouting,
        )
    }

    companion object {
        private const val TAG = "BitmovinBackend"
    }
}
