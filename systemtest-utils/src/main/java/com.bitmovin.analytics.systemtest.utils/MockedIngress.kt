package com.bitmovin.analytics.systemtest.utils

import android.util.Log
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import com.bitmovin.analytics.utils.DataSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    private val JSON_CONTENT_TYPE = "application/json; charset=utf-8".toMediaType()

    val currentImpressionsIds = mutableSetOf<String>()

    // This is a volatile variable to ensure that the value is always read from the main memory and not optimized by the compiler
    @Volatile
    private var lastRequestReceivedTimestamp = 0L

    private val httpClient = OkHttpClient()

    /**
     * This flag is used to forward the requests to the real server.
     */
    var liveServerForwarding: Boolean = true

    fun startServer(port: Int = 0): String {
        // Stop it in case it is already running and the test @After forget to stop it
        if (::server.isInitialized) {
            stopServer()
        }
        lastRequestReceivedTimestamp = System.currentTimeMillis()
        server = MockWebServer()
        server.dispatcher = dispatcher
        server.start(port)
        currentPort = server.port
        return server.url("/").toString()
    }

    fun stopServer() {
        server.shutdown()
        currentImpressionsIds.clear()
    }

    fun setServerOffline() {
        server.shutdown()
        // wait for the server to shutdown
        Thread.sleep(2000)
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
        lastRequestReceivedTimestamp = System.currentTimeMillis()
        server = MockWebServer()
        server.dispatcher = dispatcher
        server.start(currentPort)
    }

    /**
     * This method is used to track the impressions ids that are sent to the server.
     *
     * @param request The request has to be event data request.
     */
    private fun trackImpressionsIds(request: RecordedRequest) {
        val body = request.body.copy().readUtf8()
        val eventData =
            DataSerializer.deserialize(
                body,
                EventData::class.java,
            )

        if (eventData != null) {
            currentImpressionsIds.add(eventData.impressionId)
        }
    }

    private val dispatcher: Dispatcher =
        object : Dispatcher() {
            @Throws(InterruptedException::class)
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (liveServerForwarding) {
                    sendToRealServer(request)
                }
                lastRequestReceivedTimestamp = System.currentTimeMillis()
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
                    "/analytics" -> trackImpressionsIds(request)
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

        // Avoid blocking the test thread
        CoroutineScope(Dispatchers.IO).launch {
            val res = httpClient.newCall(request).execute()
            if (res.code !in 200..299) {
                Log.e("MockedIngress", "Error while forwarding requests to the real server: ${res.code}")
            }
        }
    }

    private const val MAX_TIME_BETWEEN_REQUESTS = 1000

    /**
     * This method waits for all requests to arrive at the server.
     * This avoid flakiness in the tests, as the server might not have received all requests yet.
     */
    fun waitForAllRequestsToArrive() {
        Thread.sleep(500)
        while (System.currentTimeMillis() - lastRequestReceivedTimestamp < MAX_TIME_BETWEEN_REQUESTS) {
            Thread.sleep(100)
        }
    }

    /**
     * This method waits for all requests to arrive at the server and extracts the impressions from the requests.
     * This avoid flakiness in the tests, as the server might not have received all requests yet.
     *
     * @return List of impressions extracted from the requests
     */
    fun waitForRequestsAndExtractImpressions(): List<Impression> {
        waitForAllRequestsToArrive()
        return extractImpressions()
    }

    fun extractImpressions(): List<Impression> {
        val requestCount = server.requestCount
        val eventDataMap = mutableMapOf<String, List<EventData>>()
        val adEventDataMap = mutableMapOf<String, List<AdEventDataForTest>>()
        val errorDetailMap = mutableMapOf<String, List<ErrorDetail>>()

        for (i in 0 until requestCount) {
            val request = server.takeRequest()
            val body = request.body.readUtf8()

            when (request.requestUrl?.encodedPath) {
                "/analytics" -> {
                    val eventData =
                        DataSerializer.deserialize(
                            body,
                            EventData::class.java,
                        )

                    if (request.requestUrl?.encodedQuery == "routingParam=ssai") {
                        eventData?.ssaiRelatedSample = true
                    }

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
                            AdEventDataForTest::class.java,
                        )

                    if (request.requestUrl?.encodedQuery == "routingParam=ssai") {
                        adEventData?.hasSsaiRoutingParamSet = true
                    }

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
