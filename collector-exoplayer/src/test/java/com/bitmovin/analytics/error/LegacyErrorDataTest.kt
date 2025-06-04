package com.bitmovin.analytics.error

import com.bitmovin.analytics.dtos.LegacyErrorData
import com.bitmovin.analytics.utils.DataSerializerKotlinX
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LegacyErrorDataTest {
    @Test
    fun serializeErrorData() {
        val errorData = LegacyErrorData("Our message", listOf("stack1", "stack2"))
        val serialized = DataSerializerKotlinX.serialize(errorData)
        assertThat(serialized).isEqualTo("{\"msg\":\"Our message\",\"details\":[\"stack1\",\"stack2\"]}")
    }

    @Test
    fun serializeErrorDataNoStack() {
        val errorData = LegacyErrorData("Our message", emptyList())
        val serialized = DataSerializerKotlinX.serialize(errorData)
        assertThat(serialized).isEqualTo("{\"msg\":\"Our message\",\"details\":[]}")
    }
}
