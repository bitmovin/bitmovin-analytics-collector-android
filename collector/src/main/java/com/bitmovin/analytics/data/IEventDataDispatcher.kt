package com.bitmovin.analytics.data

/**
 * Has to be greater or equal than Ingress side limit (currently 1000).
 */
const val SEQUENCE_NUMBER_LIMIT = 1000

interface IEventDataDispatcher {
    fun enable()

    fun disable()

    fun add(data: EventData)

    fun addAd(data: AdEventData)

    fun resetSourceRelatedState()
}
