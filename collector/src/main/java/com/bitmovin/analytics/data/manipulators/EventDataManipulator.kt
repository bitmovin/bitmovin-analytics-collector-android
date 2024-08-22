package com.bitmovin.analytics.data.manipulators

import com.bitmovin.analytics.data.EventData

interface EventDataManipulator {
    fun manipulate(data: EventData)

    fun manipulateForAdEvent(data: EventData) {
        manipulate(data)
    }
}
