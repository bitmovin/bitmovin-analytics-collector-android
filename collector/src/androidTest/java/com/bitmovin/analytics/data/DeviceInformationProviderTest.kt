package com.bitmovin.analytics.data

import android.os.Build
import androidx.test.InstrumentationRegistry
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DeviceInformationProviderTest {
    private val playerKey = UUID.randomUUID().toString()
    private val licenseKey = UUID.randomUUID().toString()

    private lateinit var bitmovinAnalyticsConfig: BitmovinAnalyticsConfig

    @Before
    fun setup() {
        bitmovinAnalyticsConfig = BitmovinAnalyticsConfig(licenseKey, playerKey)
    }

    @Test
    fun testContainsBuildModelAndManufacturer() {
        // #region Mocking
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val deviceInformationProvider = DeviceInformationProvider(appContext, "user-agent")
        val deviceInfo = deviceInformationProvider.getDeviceInformation()
        // #endregion

        Assertions.assertThat(deviceInfo.model).isEqualTo(Build.MODEL)
        Assertions.assertThat(deviceInfo.manufacturer).isEqualTo(Build.MANUFACTURER)
    }
}