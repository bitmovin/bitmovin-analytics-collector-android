package com.bitmovin.analytics.systemtest.utils

import android.util.Log
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import com.bitmovin.analytics.utils.DataSerializer
import org.assertj.core.api.Assertions.fail
import java.io.BufferedReader
import java.io.InputStreamReader

private const val START_MARKER = "Systemtest started"

object LogParser {

    fun startTracking() {
        Log.d("SystemTest", START_MARKER)
    }

    fun extractImpressions(): List<Impression> {
        val jsonSamples = extractHttpClientJsonLogLines()
        var currentImpression = Impression()
        val impressionList = mutableListOf(currentImpression)

        // remove license call (but keep errorDetail, evenData and adEventData, thus filter for impressionId or adImpressionId)
        jsonSamples.removeAll { x -> (!x.contains("impressionId") && !x.contains("adImpressionId")) }

        val iterator = jsonSamples.iterator()
        while (iterator.hasNext()) {
            val sample = iterator.next()

            if (sample.contains("adImpressionId")) {
                val adEventData = DataSerializer.deserialize(
                    sample,
                    AdEventData::class.java,
                )

                if (adEventData != null) {
                    currentImpression.adEventDataList.add(adEventData)
                } else {
                    fail<Nothing>("Couldn't parse ad event data")
                }

                continue
            }

            val eventData = DataSerializer.deserialize(
                sample,
                EventData::class.java,
            )

            if (eventData != null) {
                if (isNewImpressionSample(eventData) && !currentImpression.isEmpty()) {
                    currentImpression = Impression()
                    impressionList.add(currentImpression)
                }

                currentImpression.eventDataList.add(eventData)

                // if eventdata includes errorMessage, next sample is errorDetail
                if (eventData.errorMessage != null) {
                    if (!iterator.hasNext()) {
                        fail<Nothing>("No errorDetail was sent after eventData with errorMessage")
                    }

                    val errorDetailJson = iterator.next()
                    val errorDetail = DataSerializer.deserialize(
                        errorDetailJson,
                        ErrorDetail::class.java,
                    )
                    if (errorDetail != null) {
                        currentImpression.errorDetailList.add(errorDetail)
                    } else {
                        fail<Nothing>("Couldn't parse errorDetails")
                    }
                }
            } else {
                fail<Nothing>("Couldn't parse event data")
            }
        }

        return impressionList.filter { !it.isEmpty() }
    }

    private fun isNewImpressionSample(eventData: EventData): Boolean {
        return eventData.sequenceNumber == 0
    }

    private fun extractHttpClientJsonLogLines(): MutableList<String> {
        val logLines = mutableListOf<String>()
        val process = Runtime.getRuntime().exec("logcat -d")
        val bufferedReader = BufferedReader(
            InputStreamReader(process.inputStream),
        )

        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            logLines.add(line!!)
        }

        // find starting of logs of most recent test run (this is a bit of a hack because I couldn't get
        // clearing of logcat after a test run working)
        val testRunLogStartedIdx = logLines.indexOfLast { x -> x.contains(START_MARKER) }
        val testRunLines = logLines.subList(testRunLogStartedIdx, logLines.size)

        // filter for log lines that contain the network requests
        val jsonRegex = """\{.*\}$""".toRegex()
        val analyticsSamplesLogLines = testRunLines.filter { x -> x.contains("HttpClient: {") }

        // this is a hack, if json is cut off in the middle (happens on adSamples
        // due to limited logcat maxLine size) we cut off at the last "," and add a "}"
        // this way we have a valid json and can at least parse most of the sample
        // this workaround allows us to test ads without changing how we log samples in the collector
        val test = analyticsSamplesLogLines.map { x ->
            if (x.endsWith("}")) { // if sample ends with } it wasnt cut of and we don't transform it
                x
            } else {
                val index = x.lastIndexOf(",")
                x.substring(0, index) + "}"
            }
        }

        // extract the json values without the "}"
        val analyticsSamplesJsonStrings = test.map { x -> jsonRegex.find(x)?.value }
            .toMutableList()
            .filterNotNull()

        // print extracted samples to debug test errors easier
        analyticsSamplesJsonStrings.forEach { x -> println("Sample: $x") }

        return analyticsSamplesJsonStrings.toMutableList()
    }
}
