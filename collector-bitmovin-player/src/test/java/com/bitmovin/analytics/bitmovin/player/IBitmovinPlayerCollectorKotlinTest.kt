package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsConfig
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

class IBitmovinPlayerCollectorKotlinTest {

    // This test is a sanity check that the kotlin factory stays stable and consistent with the naming
    @Test
    fun testFactory_shouldCreateNewCollectorObject() {
        // arrange
        val config = BitmovinAnalyticsConfig("analytics_key", "player_key")

        // act and assert (since return value is non nullable we get a valid object or an exception with this call)
        val collector = IBitmovinPlayerCollector.Factory.create(config, mockk(relaxed = true))
        val collector2 = IBitmovinPlayerCollector.create(config, mockk(relaxed = true))
    }

    // This test is a sanity check that the kotlin factory stays stable and consistent with the naming
    @Test
    fun testFactory_shouldCreateNewCollectorObjectWithAnalyticsConfig() {
        val analyticsConfig = AnalyticsConfig("analytics_key")
        val collector = IBitmovinPlayerCollector.Factory.create(mockk(relaxed = true), analyticsConfig)
        Assert.assertEquals(analyticsConfig.licenseKey, collector.config.licenseKey)

        val collector2 = IBitmovinPlayerCollector.create(mockk(relaxed = true), analyticsConfig)
        Assert.assertEquals(analyticsConfig.licenseKey, collector2.config.licenseKey)
    }
}
