package com.bitmovin.analytics.features

import com.bitmovin.analytics.license.LicensingState
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
        val licenseKey = "test"
        featureManager.configureFeatures(
            LicensingState.Authenticated(licenseKey),
            ConfigContainer(FeatureConfig1(true), FeatureConfig2(true)),
        )
        verifyOrder {
            feature1.configured(any(), any())
            feature2.configured(any(), any())
            feature1.enabled(licenseKey)
            feature2.enabled(licenseKey)
        }
    }

    @Test
    fun testShouldDisableFeatureIfNotAuthenticated() {
        testShouldDisableFeature(LicensingState.Unauthenticated, null)
    }

    @Test
    fun testShouldDisableFeatureIfConfigIsntProvided() {
        testShouldDisableFeature(LicensingState.Authenticated("test"), ConfigContainer(null, null))
    }

    @Test
    fun testShouldDisableFeatureIfConfigIsDisabled() {
        testShouldDisableFeature(LicensingState.Authenticated("test"), ConfigContainer(FeatureConfig1(false), null))
    }

    @Test
    fun testShouldEnableFeatureIfConfigIsPresent() {
        testShouldEnableFeature(LicensingState.Authenticated("test"), ConfigContainer(FeatureConfig1(true), null))
    }

    private fun testShouldDisableFeature(authenticated: LicensingState, configContainer: ConfigContainer?) {
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
        verify(exactly = 0) { feature1.enabled(any()) }
    }

    private fun testShouldEnableFeature(state: LicensingState.Authenticated, configContainer: ConfigContainer?) {
        val feature1 = spyk(object : Feature<ConfigContainer, FeatureConfig1>() {
            override fun extractConfig(featureConfigs: ConfigContainer): FeatureConfig1? = featureConfigs.feature1
        })

        val featureManager = FeatureManager<ConfigContainer>()
        featureManager.registerFeature(feature1)
        featureManager.configureFeatures(state, configContainer)
        verifyOrder {
            feature1.configured(any(), any())
            feature1.enabled(state.licenseKey)
        }
        verify(exactly = 0) { feature1.disabled() }
    }
}
