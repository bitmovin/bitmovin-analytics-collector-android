// package com.bitmovin.analytics.data.manipulators
//
// import com.bitmovin.analytics.BitmovinAnalyticsConfig
// import com.bitmovin.analytics.TestFactory
// import com.bitmovin.analytics.adapters.PlayerAdapter
// import com.bitmovin.analytics.api.AnalyticsConfig
// import com.bitmovin.analytics.api.SourceMetadata
// import com.bitmovin.analytics.data.DeviceInformation
// import com.bitmovin.analytics.data.PlayerInfo
// import com.bitmovin.analytics.enums.PlayerType
// import io.mockk.every
// import io.mockk.mockk
// import org.assertj.core.api.Assertions
// import org.junit.Before
// import org.junit.Test
// import org.junit.runner.RunWith
// import org.mockito.junit.MockitoJUnitRunner
// import java.util.UUID
//
// @RunWith(MockitoJUnitRunner::class)
// class ManifestUrlEventDataManipulatorTest {
//    private val licenseKey = UUID.randomUUID().toString()
//    private val impressionId = UUID.randomUUID().toString()
//
//    private lateinit var deviceInformation: DeviceInformation
//
//    @Before
//    fun setup() {
//        deviceInformation =
//            DeviceInformation("myManufacturer", "myModel", false, "de", "package-name", 100, 200)
//    }
//
//    @Test
//    fun `manipulate overrides m3u8Url with value from BitmovinAnalyticsConfig if set`() {
//        // #region Mocking
//        val bitmovinAnalyticsConfigMock = AnalyticsConfig(licenseKey)
//        bitmovinAnalyticsConfigMock.m3u8Url = "https://www.my-domain.com/file.m3u8"
//        val adapter = mockk<PlayerAdapter>(relaxed = true)
//        every { adapter.sourceMetadata } returns null
//
//        val eventData = TestFactory.createEventDataFactory(
//            bitmovinAnalyticsConfigMock,
//        ).create(
//            impressionId,
//            null,
//            mockk(relaxed = true),
//            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
//        )
//        eventData.m3u8Url = "foo"
//        // #endregion
//
//        val manipulator = ManifestUrlEventDataManipulator(adapter, bitmovinAnalyticsConfigMock)
//        manipulator.manipulate(eventData)
//
//        // value from BitmovinAnalyticsConfig should overwrite if there is something set by adapter
//        Assertions.assertThat(eventData.m3u8Url).isEqualTo("https://www.my-domain.com/file.m3u8")
//    }
//
//    @Test
//    fun `manipulate overrides mpdUrl with value from BitmovinAnalyticsConfig if set`() {
//        // #region Mocking
//        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
//        bitmovinAnalyticsConfigMock.mpdUrl = "https://www.my-domain.com/file.mpd"
//        val adapter = mockk<PlayerAdapter>(relaxed = true)
//        every { adapter.currentSourceMetadata } returns null
//
//        val eventData = TestFactory.createEventDataFactory(
//            bitmovinAnalyticsConfigMock,
//        ).create(
//            impressionId,
//            null,
//            deviceInformation,
//            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
//        )
//        eventData.mpdUrl = "foo"
//        // #endregion
//
//        val manipulator = ManifestUrlEventDataManipulator(adapter, bitmovinAnalyticsConfigMock)
//        manipulator.manipulate(eventData)
//
//        // value from BitmovinAnalyticsConfig should overwrite if there is something set by adapter
//        Assertions.assertThat(eventData.mpdUrl).isEqualTo("https://www.my-domain.com/file.mpd")
//    }
//
//    @Test
//    fun `manipulate overrides progUrl with value from BitmovinAnalyticsConfig if set`() {
//        // #region Mocking
//        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
//        bitmovinAnalyticsConfigMock.progUrl = "https://www.my-domain.com/file.mp4"
//        val adapter = mockk<PlayerAdapter>(relaxed = true)
//        every { adapter.currentSourceMetadata } returns null
//
//        val eventData = TestFactory.createEventDataFactory(
//            bitmovinAnalyticsConfigMock,
//        ).create(
//            impressionId,
//            null,
//            deviceInformation,
//            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
//        )
//        eventData.progUrl = "foo"
//        // #endregion
//
//        val manipulator = ManifestUrlEventDataManipulator(adapter, bitmovinAnalyticsConfigMock)
//        manipulator.manipulate(eventData)
//
//        // value from BitmovinAnalyticsConfig should overwrite if there is something set by adapter
//        Assertions.assertThat(eventData.progUrl).isEqualTo("https://www.my-domain.com/file.mp4")
//    }
//
//    @Test
//    fun `manipulate overrides m3u8Url with value from SourceMetadata if set`() {
//        // #region Mocking
//        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
//        bitmovinAnalyticsConfigMock.m3u8Url = "https://www.my-domain.com/file.m3u8"
//        val adapter = mockk<PlayerAdapter>(relaxed = true)
//        val sourceMetadata = SourceMetadata(m3u8Url = "bar")
//        every { adapter.currentSourceMetadata } returns sourceMetadata
//
//        val eventData = TestFactory.createEventDataFactory(
//            bitmovinAnalyticsConfigMock,
//        ).create(
//            impressionId,
//            null,
//            deviceInformation,
//            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
//        )
//        eventData.m3u8Url = "foo"
//        // #endregion
//
//        val manipulator = ManifestUrlEventDataManipulator(adapter, bitmovinAnalyticsConfigMock)
//        manipulator.manipulate(eventData)
//
//        // value from SourceMetadata should overwrite if there is something set by adapter or bitmovinAnalyticsConfig
//        Assertions.assertThat(eventData.m3u8Url).isEqualTo(sourceMetadata.m3u8Url)
//    }
//
//    @Test
//    fun `manipulate overrides mpdUrl with value from SourceMetadata if set`() {
//        // #region Mocking
//        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
//        bitmovinAnalyticsConfigMock.mpdUrl = "https://www.my-domain.com/file.m3u8"
//        val adapter = mockk<PlayerAdapter>(relaxed = true)
//        val sourceMetadata = SourceMetadata(mpdUrl = "bar")
//        every { adapter.currentSourceMetadata } returns sourceMetadata
//
//        val eventData = TestFactory.createEventDataFactory(
//            bitmovinAnalyticsConfigMock,
//        ).create(
//            impressionId,
//            null,
//            deviceInformation,
//            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
//        )
//        eventData.mpdUrl = "foo"
//        // #endregion
//
//        val manipulator = ManifestUrlEventDataManipulator(adapter, bitmovinAnalyticsConfigMock)
//        manipulator.manipulate(eventData)
//
//        // value from SourceMetadata should overwrite if there is something set by adapter or bitmovinAnalyticsConfig
//        Assertions.assertThat(eventData.mpdUrl).isEqualTo(sourceMetadata.mpdUrl)
//    }
//
//    @Test
//    fun `manipulate overrides progUrl with value from SourceMetadata if set`() {
//        // #region Mocking
//        val bitmovinAnalyticsConfigMock = BitmovinAnalyticsConfig(licenseKey)
//        bitmovinAnalyticsConfigMock.progUrl = "https://www.my-domain.com/file.mp4"
//        val adapter = mockk<PlayerAdapter>(relaxed = true)
//        val sourceMetadata = SourceMetadata(progUrl = "bar")
//        every { adapter.sourceMetadata } returns sourceMetadata
//
//        val eventData = TestFactory.createEventDataFactory(
//            bitmovinAnalyticsConfigMock,
//        ).create(
//            impressionId,
//            null,
//            deviceInformation,
//            PlayerInfo("Android:Exoplayer", PlayerType.EXOPLAYER),
//        )
//        eventData.progUrl = "foo"
//        // #endregion
//
//        val manipulator = ManifestUrlEventDataManipulator(adapter, bitmovinAnalyticsConfigMock)
//        manipulator.manipulate(eventData)
//
//        // value from SourceMetadata should overwrite if there is something set by adapter or bitmovinAnalyticsConfig
//        Assertions.assertThat(eventData.progUrl).isEqualTo(sourceMetadata.progUrl)
//    }
// }
