package com.bitmovin.analytics.utils

import java.util.Queue

class QueueExtensions {
    companion object {
        fun <T> Queue<T>.limit(maxSize: Int) {
            while (size > maxSize) {
                remove()
            }
        }
    }
}
