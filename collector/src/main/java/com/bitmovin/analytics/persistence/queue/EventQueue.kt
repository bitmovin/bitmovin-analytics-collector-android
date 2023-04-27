package com.bitmovin.analytics.persistence.queue

internal interface EventQueue<T> {
    fun push(event: T)
    fun pop(): T?
    fun clear()
}
