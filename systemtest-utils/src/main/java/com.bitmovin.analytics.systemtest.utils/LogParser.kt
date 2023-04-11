package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.errordetails.ErrorDetail
import com.bitmovin.analytics.utils.DataSerializer
import org.assertj.core.api.Assertions.fail
import java.io.BufferedReader
import java.io.InputStreamReader

object LogParser {

    fun extractImpressions(): List<Impression> {
        val jsonSamples = extractHttpClientJsonLogLines()
        val impressionList = mutableListOf<Impression>()
        var currentImpression = Impression()

        // remove license call (but keep errorDetail and evenData, thus filter for impressionId)
        jsonSamples.removeAll { x -> !x.contains("impressionId") }

        val iterator = jsonSamples.iterator()
        while (iterator.hasNext()) {
            val sample = iterator.next()

            val eventData = DataSerializer.deserialize(
                sample,
                EventData::class.java,
            )

            if (eventData != null) {
                if (isNewImpressionSample(eventData)) {
                    currentImpression = Impression()
                    impressionList.add(currentImpression)
                }

                currentImpression.eventDataList.add(eventData)

                // if eventdata includes errorMessage, next sample is errorDetail
                if (eventData.errorMessage != null) {
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

        return impressionList
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
        val testRunLogStartedIdx = logLines.indexOfLast { x -> x.contains("Initialized Analytics HTTP Backend") }
        val testRunLines = logLines.subList(testRunLogStartedIdx, logLines.size)

        // filter for log lines that contain the network requests
        val jsonRegex = """\{.*\}$""".toRegex()
        val analyticsSamplesStrings = testRunLines.filter { x -> x.contains("HttpClient: {") }
            .map { x -> jsonRegex.find(x)?.value }
            .toMutableList()
            .filterNotNull()

        // print extracted samples to debug test errors easier
        analyticsSamplesStrings.forEach { x -> println("Sample: $x") }

        return analyticsSamplesStrings.toMutableList()
    }
}
