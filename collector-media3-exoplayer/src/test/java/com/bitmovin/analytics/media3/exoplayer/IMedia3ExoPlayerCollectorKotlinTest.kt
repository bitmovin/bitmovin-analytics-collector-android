package com.bitmovin.analytics.media3.exoplayer

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.media3.exoplayer.api.IMedia3ExoPlayerCollector
import io.mockk.mockk
import org.junit.Test

class IMedia3ExoPlayerCollectorKotlinTest {

    @Test
    fun testFactory_shouldCreateNewCollectorObjectWithAnalyticsConfig() {
        // arrange
        val config = AnalyticsConfig("analytics_key")
        val defaultMetadata = DefaultMetadata()

        // act and assert (since return value is non nullable we get a valid object or an exception with this call)
        val collector = IMedia3ExoPlayerCollector.Factory.create(mockk(relaxed = true), config)
        val collector2 = IMedia3ExoPlayerCollector.create(mockk(relaxed = true), config)
        val collector3 = IMedia3ExoPlayerCollector.Factory.create(mockk(relaxed = true), config, defaultMetadata)
        val collector4 = IMedia3ExoPlayerCollector.create(mockk(relaxed = true), config, defaultMetadata)
    }

    @Test
    fun testSdkVersion_shouldReturnVersionString() {
        // act
        val sdkVersion = IMedia3ExoPlayerCollector.sdkVersion
    }
}
