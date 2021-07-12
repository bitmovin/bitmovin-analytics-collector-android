package com.bitmovin.analytics.features

import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

class FeatureManagerTests {
    data class ConfigContainer(val feature1: FeatureConfig1?, val feature2: FeatureConfig2?)

    data class FeatureConfig1(override val enabled: Boolean, var param1: String? = null) : FeatureConfig
    data class FeatureConfig2(override val enabled: Boolean) : FeatureConfig

    @Test
    fun testShouldConfigureAllFeaturesBeforeCallingEnabledHook() {
        val feature1 = spyk(object : Feature<ConfigContainer, FeatureConfig1>() {
            override fun extractConfig(featureConfigs: ConfigContainer) = featureConfigs.feature1
        })
        val feature2 = spyk(object : Feature<ConfigContainer, FeatureConfig2>() {
            override fun extractConfig(featureConfigs: ConfigContainer) = featureConfigs.feature2
        })

        val featureManager = FeatureManager<ConfigContainer>()
        featureManager.registerFeature(feature1)
        featureManager.registerFeature(feature2)
        featureManager.configureFeatures(true, ConfigContainer(FeatureConfig1(true), FeatureConfig2(true)))
        verifyOrder {
            feature1.configured(any(), any())
            feature2.configured(any(), any())
            feature1.enabled()
            feature2.enabled()
        }
    }

    @Test
    fun testShouldDisableFeatureIfNotAuthenticated() {
        testShouldDisableFeature(false, null)
    }

    @Test
    fun testShouldDisableFeatureIfConfigIsntProvided() {
        testShouldDisableFeature(true, ConfigContainer(null, null))
    }

    @Test
    fun testShouldDisableFeatureIfConfigIsDisabled() {
        testShouldDisableFeature(true, ConfigContainer(FeatureConfig1(false), null))
    }

    @Test
    fun testShouldEnableFeatureIfConfigIsPresent() {
        testShouldEnableFeature(ConfigContainer(FeatureConfig1(true), null))
    }

    private fun testShouldDisableFeature(authenticated: Boolean, configContainer: ConfigContainer?) {
        val feature1 = spyk(object : Feature<ConfigContainer, FeatureConfig1>() {
            override fun extractConfig(featureConfigs: ConfigContainer): FeatureConfig1? = featureConfigs.feature1
        })

        val featureManager = FeatureManager<ConfigContainer>()
        featureManager.registerFeature(feature1)
        featureManager.configureFeatures(authenticated, configContainer)
        verifyOrder {
            feature1.configure(any(), any())
            feature1.disabled()
        }
        verify(exactly = 0) { feature1.enabled() }
    }

    private fun testShouldEnableFeature(configContainer: ConfigContainer?) {
        val feature1 = spyk(object : Feature<ConfigContainer, FeatureConfig1>() {
            override fun extractConfig(featureConfigs: ConfigContainer): FeatureConfig1? = featureConfigs.feature1
        })

        val featureManager = FeatureManager<ConfigContainer>()
        featureManager.registerFeature(feature1)
        featureManager.configureFeatures(true, configContainer)
        verifyOrder {
            feature1.configured(any(), any())
            feature1.enabled()
        }
        verify(exactly = 0) { feature1.disabled() }
    }
}
