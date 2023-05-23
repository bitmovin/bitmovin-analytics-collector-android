package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.features.errordetails.ErrorDetail

data class Impression(val eventDataList: MutableList<EventData> = mutableListOf(), val adEventDataList: MutableList<AdEventData> = mutableListOf(), val errorDetailList: MutableList<ErrorDetail> = mutableListOf())

fun Impression.isEmpty() = eventDataList.isEmpty() and adEventDataList.isEmpty() and errorDetailList.isEmpty()
