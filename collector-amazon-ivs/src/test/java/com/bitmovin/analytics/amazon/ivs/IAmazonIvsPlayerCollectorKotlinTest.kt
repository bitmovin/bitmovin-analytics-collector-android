package com.bitmovin.analytics.amazon.ivs

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.amazon.ivs.api.IAmazonIvsPlayerCollector
import io.mockk.mockk
import org.junit.Test

class IAmazonIvsPlayerCollectorKotlinTest {

    // This test is a sanity check that the kotlin factory stays stable and consistent with the naming
    @Test
    fun testFactory_shouldCreateNewCollectorObject() {
        // arrange
        val config = BitmovinAnalyticsConfig("test-analytics-key", "test-player-key")

        // act and assert (since return value is non nullable we get a valid object or an exception with this call)
        val collector = IAmazonIvsPlayerCollector.Factory.create(config, mockk(relaxed = true))
        val collector2 = IAmazonIvsPlayerCollector.create(config, mockk(relaxed = true))
    }

    @Test
    fun testSdkVersion_shouldReturnVersionString() {
        // act
        val sdkVersion = IAmazonIvsPlayerCollector.sdkVersion
    }
}
