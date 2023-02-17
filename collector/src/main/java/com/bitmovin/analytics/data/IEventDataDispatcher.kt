package com.bitmovin.analytics.data

interface IEventDataDispatcher {
    fun enable()
    fun disable()
    fun add(data: EventData)
    fun addAd(data: AdEventData)
    fun resetSourceRelatedState()
}
