package com.bitmovin.analytics.utils

import com.bitmovin.analytics.utils.QueueExtensions.Companion.limit
import java.util.LinkedList
import java.util.Queue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class QueueExtensionsTests {
    @Test
    fun testLimit() {
        val queue: Queue<String> = LinkedList<String>()
        queue.add("")
        queue.add("")
        queue.add("")
        assertThat(queue.size).isEqualTo(3)
        queue.limit(2)
        assertThat(queue.size).isEqualTo(2)
        queue.limit(0)
        assertThat(queue.size).isEqualTo(0)
    }

    @Test
    fun `limit doesn't throw exception if queue is empty`() {
        val queue: Queue<String> = LinkedList<String>()
        queue.limit(2)
    }
}
