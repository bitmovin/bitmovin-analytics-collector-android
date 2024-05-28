package com.bitmovin.analytics.data

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.license.DeferredLicenseKeyProvider
import com.bitmovin.analytics.license.LicenseKeyState
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.utils.UserAgentProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EventDataFactoryTest {
    @Test
    fun `ssai, source and default customData get merged correctly`() {
        // arrange
        // metadata
        val ssaiMetadata =
            SsaiAdMetadata(
                customData =
                    CustomData(
                        customData1 = "ssai-custom-data-1",
                        customData2 = "ssai-custom-data-2",
                        customData3 = "ssai-custom-data-3",
                    ),
            )
        val defaultCustomData =
            CustomData(customData1 = "default-custom-data-1", customData4 = "default-custom-data-4", customData5 = "default-custom-data-5")
        val sourceCustomData =
            CustomData(customData2 = "source-custom-data-2", customData4 = "source-custom-data-4", customData6 = "source-custom-data-6")

        // mocks
        val defaultMetadataMock = mockk<DefaultMetadata>(relaxed = true)
        val sourceMetadataMock = mockk<SourceMetadata>(relaxed = true)
        val ssaiServiceMock = mockk<SsaiService>(relaxed = true)
        val licenseKeyProvider = DeferredLicenseKeyProvider(MutableStateFlow<LicenseKeyState>(LicenseKeyState.Deferred))

        every { ssaiServiceMock.adMetadata } returns ssaiMetadata
        every { defaultMetadataMock.customData } returns defaultCustomData
        every { sourceMetadataMock.customData } returns sourceCustomData

        val eventDataFactory =
            EventDataFactory(
                mockk<AnalyticsConfig>(relaxed = true),
                mockk<UserIdProvider>(relaxed = true),
                mockk<UserAgentProvider>(relaxed = true),
                licenseKeyProvider,
                ssaiServiceMock,
            )
        // act
        val customData =
            eventDataFactory.create(
                "",
                sourceMetadataMock,
                defaultMetadataMock,
                mockk<DeviceInformation>(relaxed = true),
                mockk<PlayerInfo>(relaxed = true),
            )

        // assert
        assertThat(customData.customData1).isEqualTo("ssai-custom-data-1")
        assertThat(customData.customData2).isEqualTo("ssai-custom-data-2")
        assertThat(customData.customData3).isEqualTo("ssai-custom-data-3")
        assertThat(customData.customData4).isEqualTo("source-custom-data-4")
        assertThat(customData.customData5).isEqualTo("default-custom-data-5")
        assertThat(customData.customData6).isEqualTo("source-custom-data-6")
    }
}
