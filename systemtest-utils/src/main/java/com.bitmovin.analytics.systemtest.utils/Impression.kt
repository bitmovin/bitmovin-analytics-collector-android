package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.dtos.ErrorDetail
import com.bitmovin.analytics.dtos.EventData

data class Impression(
    val eventDataList: MutableList<EventData> = mutableListOf(),
    val adEventDataList: MutableList<AdEventDataForTest> = mutableListOf(),
    val errorDetailList: MutableList<ErrorDetail> = mutableListOf(),
)

fun Impression.isEmpty() = eventDataList.isEmpty() and adEventDataList.isEmpty() and errorDetailList.isEmpty()

fun List<Impression>.combineByImpressionId(): Map<String, Impression> {
    val events = flatMap { it.eventDataList }.groupBy { it.impressionId }
    val adEvents = flatMap { it.adEventDataList }.groupBy { it.videoImpressionId }

    return (events.keys + adEvents.keys).associateWith {
        Impression(
            events[it]?.toMutableList() ?: mutableListOf(),
            adEvents[it]?.toMutableList() ?: mutableListOf(),
        )
    }
}
