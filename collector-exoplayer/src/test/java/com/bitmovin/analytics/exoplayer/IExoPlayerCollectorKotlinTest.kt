package com.bitmovin.analytics.exoplayer

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector
import io.mockk.mockk
import org.junit.Test

class IExoPlayerCollectorKotlinTest {

    // This test is a sanity check that the kotlin factory stays stable and consistent with the naming
    @Test
    fun testFactory_shouldCreateNewCollectorObject() {
        // arrange
        val config = BitmovinAnalyticsConfig("analytics_key", "player_key")

        // act and assert (since return value is non nullable we get a valid object or an exception with this call)
        val collector = IExoPlayerCollector.Factory.create(config, mockk(relaxed = true))
        val collector2 = IExoPlayerCollector.create(config, mockk(relaxed = true))
    }

    @Test
    fun testSdkVersion_shouldReturnVersionString() {
        // act
        val sdkVersion = IExoPlayerCollector.sdkVersion
    }
}
