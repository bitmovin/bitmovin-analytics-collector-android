package com.bitmovin.analytics.license

import android.net.Uri
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.features.errordetails.ErrorDetailTrackingConfig
import com.bitmovin.analytics.utils.HttpClient
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
private const val TEST_LICENSE_KEY = "testLicenseKey"

class LicenseCallTests {
    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
    }

    private fun createLicenseCall(responseBody: String): DefaultLicenseCall {
        val mockedResponse = mockk<Response>()
        every { mockedResponse.body }.returns(responseBody.toResponseBody("text/json".toMediaType()))
        mockkConstructor(HttpClient::class)
        val slot = slot<Callback>()
        every { anyConstructed<HttpClient>().post(any(), any(), capture(slot)) }.answers {
            slot.captured.onResponse(mockk(), mockedResponse)
        }
        return DefaultLicenseCall(
            AnalyticsConfig("any"),
            InstantLicenseKeyProvider(TEST_LICENSE_KEY),
            mockk(relaxed = true),
        )
    }

    private fun getGrantedResponseBody(features: String) = "{\"status\": \"granted\"$features}"

    private suspend fun verifyLicenseResponse(responseBody: String, expectedAuthenticationResponse: AuthenticationResponse) {
        val licenseCall = createLicenseCall(responseBody)
        val callback = mockk<AuthenticationCallback>(relaxed = true)
        licenseCall.authenticate(callback)

        verify { callback.authenticationCompleted(expectedAuthenticationResponse) }
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithoutFeatures() = runTest {
        verifyLicenseResponse(getGrantedResponseBody(""), AuthenticationResponse.Granted(null))
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithNullFeatures() = runTest {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": null"), AuthenticationResponse.Granted(null))
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithEmptyFeatures() = runTest {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": {}"), AuthenticationResponse.Granted(FeatureConfigContainer(null)))
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithErrorTracking() = runTest {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": { \"errorDetails\": {} }"), AuthenticationResponse.Granted(FeatureConfigContainer(ErrorDetailTrackingConfig(false))))
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithDisabledErrorTracking() = runTest {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": { \"errorDetails\": {\"enabled\": false} }"), AuthenticationResponse.Granted(FeatureConfigContainer(ErrorDetailTrackingConfig(false))))
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithEnabledErrorTracking() = runTest {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": { \"errorDetails\": {\"enabled\": true, \"numberOfHttpRequests\": 12} }"), AuthenticationResponse.Granted(FeatureConfigContainer(ErrorDetailTrackingConfig(true, 12))))
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithEnabledErrorTrackingAndTypo() = runTest {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": { \"errorDetails\": {\"enabled\": true, \"numberOfSeeegments\": 12} }"), AuthenticationResponse.Granted(FeatureConfigContainer(ErrorDetailTrackingConfig(true))))
    }

    @Test
    fun testLicenseResponseShouldFailWithWrongFeatures() = runTest {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": \"asdf\""), AuthenticationResponse.Error)
    }

    @Test
    fun testLicenseResponseShouldFailWithTimedOutDeferredLicense() = runTest {
        verifyDeferredLicenseKey(LicenseKeyState.Timeout, AuthenticationResponse.Error)
    }

    @Test
    fun testLicenseResponseShouldFailWithNotProvidedDeferredLicense() = runTest {
        verifyDeferredLicenseKey(LicenseKeyState.NotProvided, AuthenticationResponse.Error)
    }

    @Test
    fun testLicenseResponseShouldFailIfDeferredLicenseINotProvidedInTime() = runTest {
        verifyDeferredLicenseKey(LicenseKeyState.Deferred, AuthenticationResponse.Error)
    }

    @Test
    fun testLicenseResponseShouldSucceedIfDeferredLicenseIsProvidedInTime() = runTest {
        verifyDeferredLicenseKey(
            LicenseKeyState.Provided("key"),
            AuthenticationResponse.Granted(null),
        )
    }

    private suspend fun verifyDeferredLicenseKey(
        state: LicenseKeyState,
        expected: AuthenticationResponse,
    ) {
        val mockedResponse = mockk<Response> {
            every { body }.returns(
                getGrantedResponseBody("").toResponseBody("text/json".toMediaType()),
            )
        }
        val callbackSlot = slot<Callback>()
        val httpClient = mockk<HttpClient> {
            every { post(any(), any(), capture(callbackSlot)) }.answers {
                callbackSlot.captured.onResponse(mockk(), mockedResponse)
            }
        }
        val licenseCall = DefaultLicenseCall(
            config = AnalyticsConfig(TEST_LICENSE_KEY),
            licenseKeyProvider = DeferredLicenseKeyProvider(MutableStateFlow(state)),
            context = mockk(),
            httpClient = httpClient,
        )
        val callback = mockk<AuthenticationCallback> {
            every { authenticationCompleted(any()) } just runs
        }

        licenseCall.authenticate(callback)

        verify { callback.authenticationCompleted(expected) }
    }
}
