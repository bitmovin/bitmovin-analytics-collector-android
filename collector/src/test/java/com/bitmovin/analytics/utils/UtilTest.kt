package com.bitmovin.analytics.utils;

import org.junit.Test
import java.lang.Exception
import org.assertj.core.api.Assertions.*

class UtilTest {

    @Test
    fun testTopOfStacktrace() {
        try {
            throw RuntimeException("RUNTIMEEXCEPTION")
        } catch (e : Exception){
            val top = e.topOfStacktrace
            assertThat(top).hasSize(10)
            assertThat(top).anySatisfy{ element -> assertThat(element).contains("testTopOfStacktrace")}
        }
    }
}
