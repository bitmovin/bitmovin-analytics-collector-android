@file:OptIn(ExperimentalCoroutinesApi::class)

package com.bitmovin.analytics.license

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

private const val LICENSE_KEY = "testLicenseKey"

class InstantLicenseKeyProviderTest {
    private val licenseKeyProvider = InstantLicenseKeyProvider(LICENSE_KEY)

    @Test
    fun `getting licenseKeyOrNull returns provided license key`() {
        assert(licenseKeyProvider.licenseKeyOrNull == LICENSE_KEY)
    }

    @Test
    fun `waiting for a result immediately returns the provided license key`() = runTest {
        val result = licenseKeyProvider.waitForResult()
        assertThat((result as LicenseKeyState.Provided).licenseKey).isEqualTo(LICENSE_KEY)
    }
}
