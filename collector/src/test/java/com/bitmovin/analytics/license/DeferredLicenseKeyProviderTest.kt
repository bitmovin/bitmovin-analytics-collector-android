@file:OptIn(ExperimentalCoroutinesApi::class)

package com.bitmovin.analytics.license

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DeferredLicenseKeyProviderTest {
    @Test
    fun `awaiting a result when no result is posted times out after 60 seconds times out`() = runTest {
        val licenseKeyFlow = MutableStateFlow<LicenseKeyState>(LicenseKeyState.Deferred)
        val licenseKeyProvider = DeferredLicenseKeyProvider(licenseKeyFlow)

        val deferred = async { licenseKeyProvider.waitForResult() }
        advanceTimeBy(59_000) // advance time by 59 seconds - shortly before the timeout
        assertThat(deferred.isCompleted).isFalse
        advanceTimeBy(2_000) // advance time over the timeout
        assertThat(deferred.isCompleted).isTrue
        assertThat(deferred.getCompleted()).isInstanceOf(LicenseKeyState.Timeout::class.java)
    }

    @Test
    fun `awaiting a result when a success result is posted returns the result`() = runTest {
        val licenseKeyFlow = MutableStateFlow<LicenseKeyState>(LicenseKeyState.Deferred)
        val licenseKeyProvider = DeferredLicenseKeyProvider(licenseKeyFlow)

        val deferred = async { licenseKeyProvider.waitForResult() }
        advanceTimeBy(59_000)
        licenseKeyFlow.emit(LicenseKeyState.Provided("testLicenseKey"))
        advanceUntilIdle()
        assertThat(deferred.getCompleted()).isEqualTo(LicenseKeyState.Provided("testLicenseKey"))
    }

    @Test
    fun `awaiting a result when a failure result is posted returns the result`() = runTest {
        val licenseKeyFlow = MutableStateFlow<LicenseKeyState>(LicenseKeyState.Deferred)
        val licenseKeyProvider = DeferredLicenseKeyProvider(licenseKeyFlow)

        val deferred = async { licenseKeyProvider.waitForResult() }
        advanceTimeBy(59_000)
        licenseKeyFlow.emit(LicenseKeyState.NotProvided)
        advanceUntilIdle()

        assertThat(deferred.getCompleted()).isEqualTo(LicenseKeyState.NotProvided)
    }
}
