package com.bitmovin.analytics.data

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowPackageManager
import org.robolectric.shadows.ShadowUIModeManager

@RunWith(
    RobolectricTestRunner::class,
)
class DeviceInformationProviderTest {

    @Test
    @Config(qualifiers = "fr-rFR-w360dp-h640dp-xhdpi")
    fun testGetDeviceInformation_Phone_targetAPI_ShouldReturnInfo() {
        // arrange
        val dip = DeviceInformationProvider(ApplicationProvider.getApplicationContext())

        // act
        val deviceInfo = dip.getDeviceInformation()

        // assert
        Assert.assertEquals("fr_FR", deviceInfo.locale)
        Assert.assertEquals(720, deviceInfo.screenWidth)
        Assert.assertEquals(1280, deviceInfo.screenHeight)

        // manufacturer and model are expected to be robolectric when running tests with the framework
        Assert.assertEquals("robolectric", deviceInfo.manufacturer)
        Assert.assertEquals("robolectric", deviceInfo.model)
        Assert.assertEquals("com.bitmovin.analytics.test", deviceInfo.domain)
        Assert.assertEquals(null, deviceInfo.deviceClass)
    }

    @Test
    @Config(qualifiers = "en-rUS-w1080dp-h1920dp-xhdpi", sdk = [26])
    fun testGetDeviceInformation_4kPhone_API_V26_ShouldReturnInfo() {
        // arrange
        val dip = DeviceInformationProvider(ApplicationProvider.getApplicationContext())

        // act
        val deviceInfo = dip.getDeviceInformation()

        // assert
        Assert.assertEquals("en_US", deviceInfo.locale)
        Assert.assertEquals(false, deviceInfo.isTV)
        Assert.assertEquals(2160, deviceInfo.screenWidth)
        Assert.assertEquals(3840, deviceInfo.screenHeight)
    }

    @Test
    @Config(qualifiers = "w1080dp-h1920dp-xhdpi-notouch-keyshidden", sdk = [31])
    fun testGetDeviceInformation_4kTV_API_V31_ShouldReturnInfo() {
        // arrange
        val context = ApplicationProvider.getApplicationContext<Context>()
        setTvMode(context)
        val dip = DeviceInformationProvider(context)

        // act
        val deviceInfo = dip.getDeviceInformation()

        // assert
        Assert.assertEquals(true, deviceInfo.isTV)
        Assert.assertEquals(DeviceClass.TV, deviceInfo.deviceClass)
        Assert.assertEquals(2160, deviceInfo.screenWidth)
        Assert.assertEquals(3840, deviceInfo.screenHeight)
    }

    @Test
    @Config(qualifiers = "w1080dp-h1920dp-xhdpi-notouch-keyshidden", sdk = [30])
    fun testGetDeviceInformation_4kTV_API_V30_ShouldReturnInfo() {
        // arrange
        val context = ApplicationProvider.getApplicationContext<Context>()
        setTvMode(context)
        val dip = DeviceInformationProvider(context)

        // act
        val deviceInfo = dip.getDeviceInformation()

        // assert
        Assert.assertEquals(true, deviceInfo.isTV)
        Assert.assertEquals(DeviceClass.TV, deviceInfo.deviceClass)
        Assert.assertEquals(2160, deviceInfo.screenWidth)
        Assert.assertEquals(3840, deviceInfo.screenHeight)
    }

    @Test
    @Config(qualifiers = "w1080dp-h1920dp-xhdpi-notouch-keyshidden", sdk = [21])
    fun testGetDeviceInformation_4kTV_API_V21_ShouldReturnInfo() {
        // arrange
        val context = ApplicationProvider.getApplicationContext<Context>()
        setTvMode(context)
        val dip = DeviceInformationProvider(context)

        // act
        val deviceInfo = dip.getDeviceInformation()

        // assert
        Assert.assertEquals(true, deviceInfo.isTV)
        Assert.assertEquals(DeviceClass.TV, deviceInfo.deviceClass)
        Assert.assertEquals(2160, deviceInfo.screenWidth)
        Assert.assertEquals(3840, deviceInfo.screenHeight)
    }

    @Test
    @Config(qualifiers = "w1080dp-h1920dp-xhdpi-notouch-keyshidden", sdk = [33])
    fun testGetDeviceInformation_FireOS_8_TV_API_V33_ShouldReturnInfo() {
        // arrange
        val context = ApplicationProvider.getApplicationContext<Context>()
        setTvMode(context)
        setFireOs(context)

        val dip = DeviceInformationProvider(context)

        // act
        val deviceInfo = dip.getDeviceInformation()

        // assert
        Assert.assertEquals(true, deviceInfo.isTV)
        Assert.assertEquals("Fire OS", deviceInfo.operatingSystem)
        Assert.assertEquals(">=8", deviceInfo.operatingSystemMajor)
        Assert.assertEquals(null, deviceInfo.operatingSystemMinor)
        Assert.assertEquals(DeviceClass.TV, deviceInfo.deviceClass)
        Assert.assertEquals(2160, deviceInfo.screenWidth)
        Assert.assertEquals(3840, deviceInfo.screenHeight)
    }

    @Test
    @Config(qualifiers = "w1080dp-h1920dp-xhdpi-notouch-keyshidden", sdk = [29])
    fun testGetDeviceInformation_FireOS_7_TV_API_V29_ShouldReturnInfo() {
        // arrange
        val context = ApplicationProvider.getApplicationContext<Context>()
        setTvMode(context)
        setFireOs(context)

        val dip = DeviceInformationProvider(context)

        // act
        val deviceInfo = dip.getDeviceInformation()

        // assert
        Assert.assertEquals(true, deviceInfo.isTV)
        Assert.assertEquals("Fire OS", deviceInfo.operatingSystem)
        Assert.assertEquals("7", deviceInfo.operatingSystemMajor)
        Assert.assertEquals(DeviceClass.TV, deviceInfo.deviceClass)
        Assert.assertEquals(2160, deviceInfo.screenWidth)
        Assert.assertEquals(3840, deviceInfo.screenHeight)
    }

    private fun setTvMode(context: Context) {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val shadowUiModeManager = Shadow.extract<Any>(uiModeManager) as ShadowUIModeManager

        // setting TV mode through shadow since through robolectrics qualifier didn't work
        shadowUiModeManager.currentModeType = Configuration.UI_MODE_TYPE_TELEVISION
    }

    private fun setFireOs(context: Context) {
        val spm: ShadowPackageManager = shadowOf(context.packageManager)
        spm.setSystemFeature("amazon.hardware.fire_tv", true)
    }
}
