package com.bitmovin.analytics.features

import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FeatureTests {
    data class TestFeatureConfigNonOptional(val testValue: Int) : FeatureConfig()
    data class TestFeatureConfigOptional(val testValue: Int?) : FeatureConfig()

    class Feature1 : Feature<TestFeatureConfigNonOptional>("feature1", TestFeatureConfigNonOptional::class)
    class Feature2 : Feature<TestFeatureConfigOptional>("feature2", TestFeatureConfigOptional::class)

    @Test
    fun testConfigureWillReturnNullOnNullStringAndCallsConfigureHook() {
        val feature = spyk(Feature1())
        val config = feature.configure(true, null)
        assertThat(config).isNull()
        verify(exactly = 1) { feature.configured(true, null) }
    }

    @Test
    fun testConfigureWillReturnNullOnEmptyConfigAndCallsConfigureHook() {
        val feature = spyk(Feature1())
        val config = feature.configure(true, "")
        assertThat(config).isNull()
        verify(exactly = 1) { feature.configured(true, null) }
    }

    @Test
    fun testConfigureWillReturnDefaultsOnPartialConfigAndCallsConfigureHook() {
        val feature = spyk(Feature1())
        val config = feature.configure(true, "{}")
        assertThat(config).isNotNull
        assertThat(config?.enabled).isFalse()
        assertThat(config?.testValue).isEqualTo(0)
        verify(exactly = 1) { feature.configured(true, config) }
    }

    @Test
    fun testConfigureWillReturnDefaultsOnPartialOptionalConfigAndCallsConfigureHook() {
        val feature = spyk(Feature2())
        val config = feature.configure(true, "{}")
        assertThat(config).isNotNull
        assertThat(config?.enabled).isFalse()
        assertThat(config?.testValue).isNull()
        verify(exactly = 1) { feature.configured(true, config) }
    }

    @Test
    fun testConfigureWillReturnDefaultsOnInvalidConfigAndCallsConfigureHook() {
        val feature = spyk(Feature1())
        val config = feature.configure(true, "{ \"enabled\": \"wrong\" }")
        assertThat(config).isNotNull
        assertThat(config?.enabled).isFalse()
        assertThat(config?.testValue).isEqualTo(0)
        verify(exactly = 1) { feature.configured(true, config) }
    }

    @Test
    fun testConfigureWillReturnDisabledConfigAndCallsConfigureHook() {
        val feature = spyk(Feature1())
        val config = feature.configure(true, "{ \"enabled\": false }")
        assertThat(config).isNotNull
        assertThat(config?.enabled).isFalse()
        assertThat(config?.testValue).isEqualTo(0)
        verify(exactly = 1) { feature.configured(true, config) }
    }

    @Test
    fun testConfigureWillReturnEnabledConfigAndCallsConfigureHook() {
        val feature = spyk(Feature1())
        val config = feature.configure(true, "{ \"enabled\": true }")
        assertThat(config).isNotNull
        assertThat(config?.enabled).isTrue()
        assertThat(config?.testValue).isEqualTo(0)
        verify(exactly = 1) { feature.configured(true, config) }
    }
}
