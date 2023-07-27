package com.bitmovin.analytics.amazon.ivs

import com.bitmovin.analytics.amazon.ivs.api.IAmazonIvsPlayerCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import io.mockk.mockk
import org.junit.Test

class IAmazonIvsPlayerCollectorKotlinTest {

    // This test is a sanity check that the kotlin factory stays stable and consistent with the naming
    @Test
    fun testFactory_shouldCreateNewCollectorObjectWithAnalyticsConfig() {
        // arrange
        val config = AnalyticsConfig("test-analytics-key")
        val defaultMetadata = DefaultMetadata()

        // act and assert (since return value is non nullable we get a valid object or an exception with this call)
        val collector = IAmazonIvsPlayerCollector.Factory.create(mockk(relaxed = true), config)
        val collector2 = IAmazonIvsPlayerCollector.create(mockk(relaxed = true), config)
        val collector3 = IAmazonIvsPlayerCollector.Factory.create(mockk(relaxed = true), config, defaultMetadata)
        val collector4 = IAmazonIvsPlayerCollector.create(mockk(relaxed = true), config, defaultMetadata)
    }

    @Test
    fun testSdkVersion_shouldReturnVersionString() {
        // act
        val sdkVersion = IAmazonIvsPlayerCollector.sdkVersion
    }
}
