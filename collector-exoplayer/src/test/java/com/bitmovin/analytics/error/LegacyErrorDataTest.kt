package com.bitmovin.analytics.error

import com.bitmovin.analytics.data.LegacyErrorData
import com.bitmovin.analytics.utils.DataSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LegacyErrorDataTest {
    @Test
    fun serializeErrorData() {
        val errorData = LegacyErrorData("Our message", listOf("stack1", "stack2"))
        val serialized = DataSerializer.serialize(errorData)
        assertThat(serialized).isEqualTo("{\"msg\":\"Our message\",\"details\":[\"stack1\",\"stack2\"]}")
    }

    @Test
    fun serializeErrorDataNoStack() {
        val errorData = LegacyErrorData("Our message", emptyList())
        val serialized = DataSerializer.serialize(errorData)
        assertThat(serialized).isEqualTo("{\"msg\":\"Our message\",\"details\":[]}")
    }
}
