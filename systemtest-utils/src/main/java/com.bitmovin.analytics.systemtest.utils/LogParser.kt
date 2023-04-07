package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.utils.DataSerializer
import java.io.BufferedReader
import java.io.InputStreamReader

object LogParser {

    fun extractAnalyticsSamplesFromLogs(): MutableList<EventData> {
        val analyticsSamplesStrings = extractHttpClientJsonLogLines()

        // remove the first network requests since it is the license call
        val licenseCallStringJson = analyticsSamplesStrings.removeFirst()

        val eventDataList = mutableListOf<EventData>()

        for (sample in analyticsSamplesStrings) {
            val eventData = DataSerializer.deserialize(
                sample,
                EventData::class.java,
            )

            if (eventData != null) {
                eventDataList.add(eventData)
            }
        }

        return eventDataList
    }

    fun extractHttpClientJsonLogLines(): MutableList<String> {
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
