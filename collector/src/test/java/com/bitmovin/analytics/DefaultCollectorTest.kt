package com.bitmovin.analytics

import android.content.Context
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.ssai.SsaiApi
import com.bitmovin.analytics.data.MetadataProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class DefaultCollectorTest {
    @Test
    fun testSetCustomData_shouldCloseSample() {
        // arrange
        val mockedMetadataProvider = mockk<MetadataProvider>(relaxed = true)
        val mockedBitmovinAnalytics = mockk<BitmovinAnalytics>(relaxed = true)
        val mockedSsaiApi = mockk<SsaiApi>(relaxed = true)

        val defaultCollector =
            DummyCollector(
                AnalyticsConfig("fakeLicense"),
                mockk(relaxed = true),
                mockedMetadataProvider,
                mockedBitmovinAnalytics,
                mockedSsaiApi,
            )
        every { mockedMetadataProvider.defaultMetadata.customData }.returns(CustomData(customData2 = "test2", customData29 = "test29"))
        every { mockedBitmovinAnalytics.activeCustomData }.returns(CustomData(customData2 = "test2", customData29 = "test29"))

        // act
        defaultCollector.customData = CustomData(customData1 = "test1", customData30 = "test30")

        // assert
        verify(exactly = 1) { mockedBitmovinAnalytics.closeCurrentSampleForCustomDataChangeIfNeeded() }
    }

    @Test
    fun testSetCustomData_shouldNotCloseSample() {
        // arrange
        val mockedMetadataProvider = mockk<MetadataProvider>(relaxed = true)
        val mockedBitmovinAnalytics = mockk<BitmovinAnalytics>(relaxed = true)
        val mockedSsaiApi = mockk<SsaiApi>(relaxed = true)

        val defaultCollector =
            DummyCollector(
                AnalyticsConfig("fakeLicense"),
                mockk(relaxed = true),
                mockedMetadataProvider,
                mockedBitmovinAnalytics,
                mockedSsaiApi,
            )
        every { mockedMetadataProvider.defaultMetadata.customData }.returns(CustomData(customData2 = "test2", customData29 = "test29"))
        every { mockedBitmovinAnalytics.activeCustomData }.returns(CustomData(customData2 = "test2", customData29 = "test29"))

        // act
        defaultCollector.customData = CustomData(customData2 = "test2", customData29 = "test29")

        // assert
        verify(exactly = 0) { mockedBitmovinAnalytics.closeCurrentSampleForCustomDataChangeIfNeeded() }
    }

    private class DummyPlayer

    private class DummyCollector(
        analyticsConfig: AnalyticsConfig,
        context: Context,
        metadataProvider: MetadataProvider,
        val bitmovinAnalytics: BitmovinAnalytics,
        val ssaiApi: SsaiApi,
    ) : DefaultCollector<DummyPlayer>(analyticsConfig, context, metadataProvider) {
        override fun createAdapter(
            player: DummyPlayer,
            analytics: BitmovinAnalytics,
        ): PlayerAdapter {
            throw NotImplementedError()
        }

        override val ssai: SsaiApi
            get() = this.ssaiApi

        override val analytics: BitmovinAnalytics
            get() = this.bitmovinAnalytics
    }
}
