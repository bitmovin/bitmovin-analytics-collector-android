package com.bitmovin.analytics.license

import android.net.Uri
import com.bitmovin.analytics.BitmovinAnalyticsConfig
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

    private fun getGrantedResponseBody(settings: String) = "{\"status\": \"granted\"$settings}"

    private fun verifyLicenseResponse(responseBody: String, expectedSuccess: Boolean, expectedSettings: Map<String, String>?) {
        val licenseCall = createLicenseCall(responseBody)
        val callback = mockk<AuthenticationCallback>(relaxed = true)
        licenseCall.authenticate(callback)
        verify { callback.authenticationCompleted(expectedSuccess, expectedSettings) }
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithoutSettings() {
        verifyLicenseResponse(getGrantedResponseBody(""), true, null)
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithNullSettings() {
        verifyLicenseResponse(getGrantedResponseBody(", \"settings\": null"), true, null)
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithEmptySettings() {
        verifyLicenseResponse(getGrantedResponseBody(", \"settings\": {}"), true, HashMap())
    }

    @Test
    fun testLicenseResponseShouldSuccessfullyBeParsedWithCorrectSettings() {
        verifyLicenseResponse(getGrantedResponseBody(", \"settings\": { \"feature\": \"foo\" }"), true, hashMapOf("feature" to "foo"))
    }

    @Test
    fun testLicenseResponseShouldFailWithWrongSettings() {
        verifyLicenseResponse(getGrantedResponseBody(", \"settings\": \"asdf\""), false, null)
    }
}
