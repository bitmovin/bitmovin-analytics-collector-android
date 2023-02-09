package com.bitmovin.analytics.bitmovin.player.providers

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.bitmovin.player.api.PlayerConfig
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PlayerLicenseProviderTest {

    @Test
    fun testGetBitmovinPlayerLicenseKey_ShouldReturnLicenseFromPlayerConfig() {
        // arrange
        val contextMock = mockk<Context>(relaxed = true)
        val provider = PlayerLicenseProvider(contextMock)
        val config = PlayerConfig(key = "testKey")

        // act
        val key = provider.getBitmovinPlayerLicenseKey(config)

        // assert
        assertThat(key).isEqualTo("testKey")
    }

    @Test
    fun testGetBitmovinPlayerLicenseKey_ShouldReturnLicenseFromManifest() {
        // arrange
        val contextMock = mockk<Context>(relaxed = true)
        val metaDataMock = mockk<Bundle>(relaxed = true)
        val provider = PlayerLicenseProvider(contextMock)
        val config = PlayerConfig()

        val applicationInfo = ApplicationInfo()
        applicationInfo.metaData = metaDataMock

        @Suppress("DEPRECATION")
        every { contextMock.packageManager.getApplicationInfo(any(), PackageManager.GET_META_DATA) } returns applicationInfo
        every { metaDataMock.getString("BITMOVIN_PLAYER_LICENSE_KEY") } returns "license key"

        // act
        val key = provider.getBitmovinPlayerLicenseKey(config)

        // assert
        assertThat(key).isEqualTo("license key")
    }
}
