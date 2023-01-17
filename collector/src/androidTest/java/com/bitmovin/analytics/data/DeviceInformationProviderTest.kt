package com.bitmovin.analytics.data

import android.os.Build
import androidx.test.InstrumentationRegistry
import org.assertj.core.api.Assertions
import org.junit.Test

class DeviceInformationProviderTest {
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
