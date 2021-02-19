package com.bitmovin.analytics.utils

import com.bitmovin.analytics.utils.QueueExtensions.Companion.limit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

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
}
