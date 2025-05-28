package com.bitmovin.analytics.data.manipulators

import com.bitmovin.analytics.dtos.EventData

interface EventDataManipulator {
    fun manipulate(data: EventData)

    fun manipulateForAdEvent(data: EventData) {
        manipulate(data)
    }
}
