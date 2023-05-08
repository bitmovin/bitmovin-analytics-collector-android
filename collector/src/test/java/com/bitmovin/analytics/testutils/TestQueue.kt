package com.bitmovin.analytics.testutils

import com.bitmovin.analytics.persistence.queue.EventQueue
import java.util.LinkedList
import java.util.Queue

internal class TestQueue<T> : EventQueue<T> {
    private val queue: Queue<T> = LinkedList()

    override fun push(event: T) {
        queue.offer(event)
    }

    override fun pop() = queue.poll()

    override fun clear() = queue.clear()
}
