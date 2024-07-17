package com.bitmovin.analytics.systemtest.utils

import android.util.Log
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import com.bitmovin.analytics.utils.DataSerializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

object MockedIngress {
    private lateinit var server: MockWebServer
    private var currentPort: Int = 0
    private var backendUrl =
        "https://analytics-ingress-global.bitmovin.com"

    val JSON_CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()

    val httpClient = OkHttpClient()

    const val SERVER_FORWARDING: Boolean = true

    fun startServer(port: Int = 0): String {
        if (::server.isInitialized) {
            server.shutdown()
        }
        server = MockWebServer()
        server.dispatcher = dispatcher
        server.start(port)
        currentPort = server.port
        return server.url("/").toString()
    }

    fun stopServer() {
        server.shutdown()
    }

    fun setServerOffline() {
        server.shutdown()
        Thread.sleep(1000)
    }

    fun hasNoSamplesReceived(): Boolean {
        return server.requestCount == 0
    }

    fun requestCount(): Int {
        return server.requestCount
    }

    fun setServerOnline() {
        if (::server.isInitialized) {
            server.shutdown()
        }
        server = MockWebServer()
        server.dispatcher = dispatcher
        server.start(currentPort)
    }

    private val dispatcher: Dispatcher =
        object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (SERVER_FORWARDING) {
                    sendToRealServer(request)
                }
                when (request.path) {
                    "/licensing" ->
                        return if (request.body.readUtf8().contains("nonExistingKey")) {
                            MockResponse().setResponseCode(403)
                                .setBody(
                                    """
                                    {"status":"denied","message":"License key not found."}
                                    """.trimIndent(),
                                )
                        } else {
                            MockResponse().setResponseCode(200)
                                .setBody(
                                    """
                                    {
                                        "status": "granted",
                                        "message": "There you go.",
                                        "features": {
                                            "errorDetails": {
                                                "enabled": true,
                                                "numberOfHttpRequests": 10
                                            }
                                        }
                                    }
                                    """.trimIndent(),
                                )
                        }
                }
                return MockResponse().setResponseCode(200)
            }
        }

    private fun sendToRealServer(recordedRequest: RecordedRequest) {
        val body = recordedRequest.body.copy().readUtf8()
        val request =
            Request.Builder()
                .url(backendUrl + recordedRequest.path)
                .headers(recordedRequest.headers)
                .post(body.toRequestBody(JSON_CONTENT_TYPE))
                .build()

        val res = httpClient.newCall(request).execute()
        if (res.code !in 200..299) {
            Log.e("MockedIngress", "Error while forwarding requests to the real server: ${res.code}")
        }
    }

    fun extractImpressions(): List<Impression> {
        val requestCount = server.requestCount
        val eventDataMap = mutableMapOf<String, List<EventData>>()
        val adEventDataMap = mutableMapOf<String, List<AdEventData>>()
        val errorDetailMap = mutableMapOf<String, List<ErrorDetail>>()

        for (i in 0 until requestCount) {
            val request = server.takeRequest()
            val body = request.body.readUtf8()

            when (request.path) {
                "/analytics" -> {
                    val eventData =
                        DataSerializer.deserialize(
                            body,
                            EventData::class.java,
                        )

                    if (eventData != null) {
                        eventDataMap[eventData.impressionId]?.let {
                            eventDataMap.put(eventData.impressionId, it.plus(eventData))
                        } ?: eventDataMap.put(eventData.impressionId, listOf(eventData))
                    }
                }
                "/analytics/a" -> {
                    val adEventData =
                        DataSerializer.deserialize(
                            body,
                            AdEventData::class.java,
                        )

                    if (adEventData != null) {
                        adEventDataMap[adEventData.videoImpressionId]?.let {
                            adEventDataMap.put(adEventData.videoImpressionId, it.plus(adEventData))
                        } ?: adEventDataMap.put(adEventData.videoImpressionId, listOf(adEventData))
                    }
                }
                "/analytics/error" -> {
                    val errorDetail =
                        DataSerializer.deserialize(
                            body,
                            ErrorDetail::class.java,
                        )

                    if (errorDetail != null) {
                        errorDetailMap[errorDetail.impressionId]?.let {
                            errorDetailMap.put(errorDetail.impressionId, it.plus(errorDetail))
                        } ?: errorDetailMap.put(errorDetail.impressionId, listOf(errorDetail))
                    }
                }
            }
        }

        val impressionList = mutableListOf<Impression>()

        if (eventDataMap.isEmpty()) {
            for (entry in adEventDataMap.entries) {
                val impression = Impression()
                val adEventDataList = adEventDataMap[entry.key] ?: listOf()
                adEventDataList.sortedBy { it.time }
                impression.adEventDataList.addAll(adEventDataList)

                val errorDetailList = errorDetailMap[entry.key] ?: listOf()
                errorDetailList.sortedBy { it.timestamp }
                impression.errorDetailList.addAll(errorDetailList)
                impressionList.add(impression)
            }
        } else {
            for (entry in eventDataMap.entries) {
                val impression = Impression()
                val eventDataList = entry.value.sortedBy { it.sequenceNumber }
                impression.eventDataList.addAll(eventDataList)

                val adEventDataList = adEventDataMap[entry.key] ?: listOf()
                adEventDataList.sortedBy { it.time }
                impression.adEventDataList.addAll(adEventDataList)

                val errorDetailList = errorDetailMap[entry.key] ?: listOf()
                errorDetailList.sortedBy { it.timestamp }
                impression.errorDetailList.addAll(errorDetailList)

                impressionList.add(impression)
            }
        }

        for (impression in impressionList) {
            impression.eventDataList.forEach {
                Log.d("MockedIngress", "Sample: $it")
            }

            impression.adEventDataList.forEach {
                Log.d("MockedIngress", "AdSample: $it")
            }

            impression.errorDetailList.forEach {
                Log.d("MockedIngress", "ErrorDetailSample: $it")
            }
        }

        return impressionList
    }
}
