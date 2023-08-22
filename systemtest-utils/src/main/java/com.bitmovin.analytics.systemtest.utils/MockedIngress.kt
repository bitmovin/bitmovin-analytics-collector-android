package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import com.bitmovin.analytics.utils.DataSerializer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

object MockedIngress {
    private lateinit var server: MockWebServer
    private var currentPort: Int = 0

    fun startServer(port: Int = 0): String {
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
        server = MockWebServer()
        server.dispatcher = dispatcher
        server.start(currentPort)
    }

    private val dispatcher: Dispatcher = object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {
            when (request.path) {
                "/licensing" ->
                    return if (request.body.readUtf8().contains("nonExistingKey")) {
                        MockResponse().setResponseCode(403)
                            .setBody("""{"status":"denied","message":"License key not found."}""")
                    } else {
                        MockResponse().setResponseCode(200).setBody("""{"status":"granted","message":"There you go.","features":{"errorDetails":{"enabled":true,"numberOfHttpRequests":10}}}""")
                    }
            }
            return MockResponse().setResponseCode(200)
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
                    val eventData = DataSerializer.deserialize(
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
                    val adEventData = DataSerializer.deserialize(
                        body,
                        AdEventData::class.java,
                    )

                    if (adEventData != null) {
                        adEventDataMap[adEventData.adImpressionId]?.let {
                            adEventDataMap.put(adEventData.videoImpressionId, it.plus(adEventData))
                        } ?: adEventDataMap.put(adEventData.videoImpressionId, listOf(adEventData))
                    }
                }
                "/analytics/error" -> {
                    val errorDetail = DataSerializer.deserialize(
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
        return impressionList
    }
}
