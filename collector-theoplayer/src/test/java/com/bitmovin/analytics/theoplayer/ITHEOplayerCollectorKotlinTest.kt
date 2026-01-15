package com.bitmovin.analytics.theoplayer

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.theoplayer.api.ITHEOplayerCollector
import io.mockk.mockk
import org.junit.Test

class ITHEOplayerCollectorKotlinTest {
    @Test
    fun testFactory_shouldCreateNewCollectorObjectWithAnalyticsConfig() {
        // arrange
        val config = AnalyticsConfig("analytics_key")
        val defaultMetadata = DefaultMetadata()

        // act and assert (since return value is non nullable we get a valid object or an exception with this call)
        val collector = ITHEOplayerCollector.Factory.create(mockk(relaxed = true), config)
        val collector2 = ITHEOplayerCollector.create(mockk(relaxed = true), config)
        val collector3 = ITHEOplayerCollector.Factory.create(mockk(relaxed = true), config, defaultMetadata)
        val collector4 = ITHEOplayerCollector.create(mockk(relaxed = true), config, defaultMetadata)
    }

    @Test
    fun testSdkVersion_shouldReturnVersionString() {
        // act
        val sdkVersion = ITHEOplayerCollector.sdkVersion
    }
}
