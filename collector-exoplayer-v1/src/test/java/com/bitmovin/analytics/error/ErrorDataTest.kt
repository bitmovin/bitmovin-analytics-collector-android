package com.bitmovin.analytics.error

import com.bitmovin.analytics.data.ErrorData
import com.bitmovin.analytics.utils.DataSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ErrorDataTest {
    @Test
    fun serializeErrorData() {
        val errorData = ErrorData("Our message", arrayOf("stack1", "stack2"))
        val serialized = DataSerializer.serialize(errorData)
        assertThat(serialized).isEqualTo("{\"msg\":\"Our message\",\"details\":[\"stack1\",\"stack2\"]}")
    }

    @Test
    fun serializeErrorDataNoStack() {
        val errorData = ErrorData("Our message", emptyArray())
        val serialized = DataSerializer.serialize(errorData)
        assertThat(serialized).isEqualTo("{\"msg\":\"Our message\",\"details\":[]}")
    }
}
