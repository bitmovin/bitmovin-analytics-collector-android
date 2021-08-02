package com.bitmovin.analytics.license

import android.net.Uri
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.features.errordetails.ErrorDetailTrackingConfig
import com.bitmovin.analytics.utils.HttpClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test

class LicenseCallTests {
    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
    }

    private fun createLicenseCall(responseBody: String): LicenseCall {
        val mockedResponse = mockk<Response>()
        every { mockedResponse.body() }.returns(ResponseBody.create(MediaType.get("text/json"), responseBody))
        mockkConstructor(HttpClient::class)
        val slot = slot<Callback>()
        every { anyConstructed<HttpClient>().post(any(), any(), capture(slot)) }.answers {
            slot.captured.onResponse(mockk(), mockedResponse)
        }
        return LicenseCall(BitmovinAnalyticsConfig(""), mockk(relaxed = true))
    }

    private fun getGrantedResponseBody(features: String) = "{\"status\": \"granted\"$features}"

    private fun verifyLicenseResponse(responseBody: String, expectedSuccess: Boolean, expectedFeatures: FeatureConfigContainer?) {
        val licenseCall = createLicenseCall(responseBody)
        val callback = mockk<AuthenticationCallback>(relaxed = true)
        licenseCall.authenticate(callback)
        verify { callback.authenticationCompleted(expectedSuccess, expectedFeatures) }
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithoutFeatures() {
        verifyLicenseResponse(getGrantedResponseBody(""), true, null)
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithNullFeatures() {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": null"), true, null)
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithEmptyFeatures() {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": {}"), true, FeatureConfigContainer(null))
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithErrorTracking() {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": { \"errorDetails\": {} }"), true, FeatureConfigContainer(ErrorDetailTrackingConfig(false)))
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithDisabledErrorTracking() {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": { \"errorDetails\": {\"enabled\": false} }"), true, FeatureConfigContainer(ErrorDetailTrackingConfig(false)))
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithEnabledErrorTracking() {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": { \"errorDetails\": {\"enabled\": true, \"numberOfHttpRequests\": 12} }"), true, FeatureConfigContainer(ErrorDetailTrackingConfig(true, 12)))
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithEnabledErrorTrackingAndTypo() {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": { \"errorDetails\": {\"enabled\": true, \"numberOfSeeegments\": 12} }"), true, FeatureConfigContainer(ErrorDetailTrackingConfig(true)))
    }

    @Test
    fun testLicenseResponseShouldFailWithWrongFeatures() {
        verifyLicenseResponse(getGrantedResponseBody(", \"features\": \"asdf\""), false, null)
    }
}
