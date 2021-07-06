package com.bitmovin.analytics.features

import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

class FeatureManagerTests {
    class FeatureConfig1 : FeatureConfig() {
        var param1: String? = null
    }
    class FeatureConfig2 : FeatureConfig()

    @Test
    fun testShouldConfigureAllFeaturesBeforeCallingEnabledHook() {
        val feature1 = spyk(object : Feature<FeatureConfig1>("feature1", FeatureConfig1::class) {})
        val feature2 = spyk(object : Feature<FeatureConfig2>("feature2", FeatureConfig2::class) {})

        val featureManager = FeatureManager()
        featureManager.registerFeature(feature1)
        featureManager.registerFeature(feature2)
        featureManager.configureFeatures(true, mapOf("feature1" to "{\"enabled\": true}", "feature2" to "{\"enabled\": true}"))
        verifyOrder {
            feature1.configured(any(), any())
            feature2.configured(any(), any())
            feature1.enabled()
            feature2.enabled()
        }
    }

    @Test
    fun testShouldDisableFeatureIfNotAuthenticated() {
        testShouldDisableFeature(false, mapOf())
    }

    @Test
    fun testShouldDisableFeatureIfConfigIsntProvided() {
        testShouldDisableFeature(true, mapOf())
    }

    @Test
    fun testShouldDisableFeatureIfConfigDoesntContainEnabled() {
        testShouldDisableFeature(true, mapOf("feature1" to "{}"))
    }

    @Test
    fun testShouldDisableFeatureIfConfigHasEnabledFalse() {
        testShouldDisableFeature(true, mapOf("feature1" to "{\"enabled\": false}"))
    }

    @Test
    fun testShouldDisableFeatureIfConfigCantBeDeserialized() {
        testShouldDisableFeature(true, mapOf("feature1" to "0"))
    }

    @Test
    fun testShouldEnableFeatureIfConfigIsPresent() {
        testShouldEnableFeature(mapOf("feature1" to "{\"enabled\": true, \"param1\": null}"))
    }

    @Test
    fun testShouldEnableFeatureIfConfigIsPresentButNotComplete() {
        testShouldEnableFeature(mapOf("feature1" to "{\"enabled\": true}"))
    }

    private fun testShouldDisableFeature(authenticated: Boolean, features: Map<String, String>) {
        val feature1 = spyk(object : Feature<FeatureConfig1>("feature1", FeatureConfig1::class) {})

        val featureManager = FeatureManager()
        featureManager.registerFeature(feature1)
        featureManager.configureFeatures(authenticated, features)
        verifyOrder {
            feature1.configure(any(), any())
            feature1.disabled()
        }
        verify(exactly = 0) { feature1.enabled() }
    }

    private fun testShouldEnableFeature(features: Map<String, String>) {
        val feature1 = spyk(object : Feature<FeatureConfig1>("feature1", FeatureConfig1::class) {})

        val featureManager = FeatureManager()
        featureManager.registerFeature(feature1)
        featureManager.configureFeatures(true, features)
        verifyOrder {
            feature1.configured(any(), any())
            feature1.enabled()
        }
        verify(exactly = 0) { feature1.disabled() }
    }
}
