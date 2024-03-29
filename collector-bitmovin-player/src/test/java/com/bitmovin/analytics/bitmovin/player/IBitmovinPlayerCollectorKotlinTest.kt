package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import io.mockk.mockk
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
        IBitmovinPlayerCollector.Factory.create(mockk(relaxed = true), analyticsConfig)
        IBitmovinPlayerCollector.create(mockk(relaxed = true), analyticsConfig)

        val defaultMetadata = DefaultMetadata()
        IBitmovinPlayerCollector.Factory.create(mockk(relaxed = true), analyticsConfig, defaultMetadata)
        IBitmovinPlayerCollector.create(mockk(relaxed = true), analyticsConfig, defaultMetadata)
    }

    @Test
    fun testSdkVersion_shouldReturnVersionString() {
        // act
        val sdkVersion = IBitmovinPlayerCollector.sdkVersion
    }
}
