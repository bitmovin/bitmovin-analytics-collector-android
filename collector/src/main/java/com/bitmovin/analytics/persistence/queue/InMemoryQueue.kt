package com.bitmovin.analytics.persistence.queue

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList
import java.util.Queue

internal class InMemoryQueue<T> : SimpleQueue<T> {
    private val queue: Queue<T> = LinkedList()
    private val mutex: Mutex = Mutex()

    override fun push(event: T) {
        runBlocking {
            mutex.withLock {
                queue.offer(event)
            }
        }
    }

    override fun pop(): T? {
        return runBlocking {
            mutex.withLock {
                queue.poll()
            }
        }
    }

    override fun clear() {
        runBlocking {
            mutex.withLock {
                queue.clear()
            }
        }
    }
}
