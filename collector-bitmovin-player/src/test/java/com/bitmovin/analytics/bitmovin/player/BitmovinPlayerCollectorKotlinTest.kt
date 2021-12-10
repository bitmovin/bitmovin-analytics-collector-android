package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import org.junit.Test
import org.junit.rules.ExpectedException




class BitmovinPlayerCollectorKotlinTest {
    @Test(expected = IllegalArgumentException::class)
    fun testDeprecatedConstructorChecksForNullInConfiguration() {
        val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("foo-bar")
        BitmovinPlayerCollector(bitmovinAnalyticsConfig)
    }
}
