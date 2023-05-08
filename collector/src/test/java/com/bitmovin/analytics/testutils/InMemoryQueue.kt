package com.bitmovin.analytics.testutils

import com.bitmovin.analytics.persistence.queue.EventQueue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList
import java.util.Queue

internal class InMemoryQueue<T> : EventQueue<T> {
    private val queue: Queue<T> = LinkedList()
    private val mutex: Mutex = Mutex()

    override fun push(event: T): Unit = runBlocking {
        mutex.withLock {
            queue.offer(event)
        }
    }

    override fun pop() = runBlocking {
        mutex.withLock {
            queue.poll()
        }
    }

    override fun clear() = runBlocking {
        mutex.withLock {
            queue.clear()
        }
    }
}
