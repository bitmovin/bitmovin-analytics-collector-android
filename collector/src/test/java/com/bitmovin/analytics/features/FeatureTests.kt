package com.bitmovin.analytics.features

import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FeatureTests {
    data class ConfigContainer(val feature1: TestFeatureConfigNonOptional?, val feature2: TestFeatureConfigOptional?)

    data class TestFeatureConfigNonOptional(override val enabled: Boolean, val testValue: Int) : FeatureConfig
    data class TestFeatureConfigOptional(override val enabled: Boolean, val testValue: Int?) : FeatureConfig

    class Feature1 : Feature<ConfigContainer, TestFeatureConfigNonOptional>() {
        override fun extractConfig(featureConfigs: ConfigContainer) = featureConfigs.feature1
    }
    class Feature2 : Feature<ConfigContainer, TestFeatureConfigOptional>() {
        override fun extractConfig(featureConfigs: ConfigContainer) = featureConfigs.feature2
    }

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
        val config = feature.configure(true, ConfigContainer(null, null))
        assertThat(config).isNull()
        verify(exactly = 1) { feature.configured(true, null) }
    }

    @Test
    fun testConfigureWillReturnDefaultsOnPartialConfigAndCallsConfigureHook() {
        val feature = spyk(Feature1())
        val config = feature.configure(true, ConfigContainer(TestFeatureConfigNonOptional(false, 0), null))
        assertThat(config).isNotNull
        assertThat(config?.enabled).isFalse()
        assertThat(config?.testValue).isEqualTo(0)
        verify(exactly = 1) { feature.configured(true, config) }
    }

    @Test
    fun testConfigureWillReturnDefaultsOnPartialOptionalConfigAndCallsConfigureHook() {
        val feature = spyk(Feature2())
        val config = feature.configure(true, ConfigContainer(null, TestFeatureConfigOptional(false, null)))
        assertThat(config).isNotNull
        assertThat(config?.enabled).isFalse()
        assertThat(config?.testValue).isNull()
        verify(exactly = 1) { feature.configured(true, config) }
    }

    @Test
    fun testConfigureWillReturnDisabledConfigAndCallsConfigureHook() {
        val feature = spyk(Feature1())
        val config = feature.configure(true, ConfigContainer(TestFeatureConfigNonOptional(false,0), null))
        assertThat(config).isNotNull
        assertThat(config?.enabled).isFalse()
        assertThat(config?.testValue).isEqualTo(0)
        verify(exactly = 1) { feature.configured(true, config) }
    }

    @Test
    fun testConfigureWillReturnEnabledConfigAndCallsConfigureHook() {
        val feature = spyk(Feature1())
        val config = feature.configure(true, ConfigContainer(TestFeatureConfigNonOptional(true,0), null))
        assertThat(config).isNotNull
        assertThat(config?.enabled).isTrue()
        assertThat(config?.testValue).isEqualTo(0)
        verify(exactly = 1) { feature.configured(true, config) }
    }
}
