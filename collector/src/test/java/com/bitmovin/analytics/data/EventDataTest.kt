package com.bitmovin.analytics.data

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.enums.CDNProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventDataTest {
    private lateinit var bitmovinAnalyticsConfig: BitmovinAnalyticsConfig

    @Before
    fun setup() {
        bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac", "18ca6ad5-9768-4129-bdf6-17685e0d14d2")
    }

    @Test
    fun testEventDataContainsDeviceInformation() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val deviceInformationProvider = DeviceInformationProvider(appContext, "user-agent")
        val deviceInfo = deviceInformationProvider.getDeviceInformation()

        assertThat(deviceInfo.model).isEqualTo(Build.MODEL)
        assertThat(deviceInfo.manufacturer).isEqualTo(Build.MANUFACTURER)
    }
}
