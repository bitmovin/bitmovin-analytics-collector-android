package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.data.EventData

object EventDataUtils {
    // TODO: refactor to return new list, instead of modifying the input list
    fun filterNonDeterministicEvents(eventDataList: MutableList<EventData>) {
        // We filter for qualitychange and buffering events
        // since they are non deterministic and would probably make the test flaky
        eventDataList.removeAll { x -> x.state?.lowercase() == DataVerifier.QUALITYCHANGE }
        eventDataList.removeAll { x -> x.state?.lowercase() == DataVerifier.BUFFERING }
    }

    /**
     * Returns the first startup event from the list
     * This function assumes that there is only one startup event in the list and take the first one.
     * @param eventDataList List of EventData
     * @return EventData
     * @throws NoSuchElementException if there is no startup event in the list
     */
    fun getStartupEvent(eventDataList: MutableList<EventData>): EventData {
        return eventDataList.first { x -> x.state?.lowercase() == DataVerifier.STARTUP }
    }
}
