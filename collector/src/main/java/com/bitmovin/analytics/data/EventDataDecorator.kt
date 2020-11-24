package com.bitmovin.analytics.data

interface EventDataDecorator {
    fun decorate(data: EventData)
}