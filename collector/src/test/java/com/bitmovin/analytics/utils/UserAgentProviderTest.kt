package com.bitmovin.analytics.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(
    RobolectricTestRunner::class,
)
class UserAgentProviderTest {

    @Test
    @Config(sdk = [33])
    fun `userAgentProvider_returnsDefaultValues`() {
        // arrange
        val userAgentProvider = UserAgentProvider(null, null, null)

        // act & assert
        Assert.assertEquals("Unknown/? (Linux;Android 13)", userAgentProvider.userAgent)
    }

    @Test
    @Config(sdk = [33])
    fun `userAgentProvider_generatesUserAgentWhenSystemPropertyIsNull_onSdk33`() {
        // arrange
        val applicationInfo = ApplicationInfo()
        applicationInfo.labelRes = 0
        applicationInfo.nonLocalizedLabel = "test"
        val packageInfo = PackageInfo()
        packageInfo.versionName = "1"
        val userAgentProvider = UserAgentProvider(applicationInfo, packageInfo, null)

        // act & assert
        Assert.assertEquals("test/1 (Linux;Android 13)", userAgentProvider.userAgent)
    }

    @Test
    @Config(sdk = [21])
    fun `userAgentProvider_generatesUserAgentWhenSystemPropertyIsNull_onMinSdk`() {
        // arrange
        val applicationInfo = ApplicationInfo()
        applicationInfo.labelRes = 0
        applicationInfo.nonLocalizedLabel = "test"
        val packageInfo = PackageInfo()
        packageInfo.versionName = "1"
        val userAgentProvider = UserAgentProvider(applicationInfo, packageInfo, null)

        // act & assert
        Assert.assertEquals("test/1 (Linux;Android 5.0.2)", userAgentProvider.userAgent)
    }

    @Test
    @Config(sdk = [33])
    fun `userAgentProvider_returnsValueFromSystemProperty`() {
        // arrange
        val applicationInfo = ApplicationInfo()
        val packageInfo = PackageInfo()
        val userAgentProvider = UserAgentProvider(applicationInfo, packageInfo, "test-user-agent")

        // act & assert
        Assert.assertEquals("test-user-agent", userAgentProvider.userAgent)
    }
}
