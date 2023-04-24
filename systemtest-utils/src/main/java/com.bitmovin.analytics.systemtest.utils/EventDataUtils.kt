package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.data.EventData

object EventDataUtils {

    fun filterNonDeterministicEvents(eventDataList: MutableList<EventData>) {
        // We filter for qualitychange and buffering events
        // since they are non deterministic and would probably make the test flaky
        eventDataList.removeAll { x -> x.state?.lowercase() == DataVerifier.QUALITYCHANGE }
        eventDataList.removeAll { x -> x.state?.lowercase() == DataVerifier.BUFFERING }
    }
}
