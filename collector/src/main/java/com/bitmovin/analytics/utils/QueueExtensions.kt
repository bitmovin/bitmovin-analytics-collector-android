package com.bitmovin.analytics.utils

import java.util.Queue

class QueueExtensions {
    companion object {

        // This is not threadsafe!!, use only in combination with locking in concurrent context
        fun <T> Queue<T>.limit(maxSize: Int) {
            while (size > maxSize) {
                // poll is used because it doesn't throw exception if queue is empty
                poll()
            }
        }
    }
}
