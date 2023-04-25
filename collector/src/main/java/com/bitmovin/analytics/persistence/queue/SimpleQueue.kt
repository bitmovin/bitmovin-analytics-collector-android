package com.bitmovin.analytics.persistence.queue

internal interface SimpleQueue<T> {
    fun push(event: T)
    fun pop(): T?
    fun clear()
}
