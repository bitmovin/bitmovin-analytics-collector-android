package com.bitmovin.analytics.license

import android.content.Context
import android.net.Uri
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.LicenseCallData
import com.bitmovin.analytics.data.LicenseResponse
import com.bitmovin.analytics.license.AuthenticationResponse.Denied
import com.bitmovin.analytics.license.AuthenticationResponse.Error
import com.bitmovin.analytics.license.AuthenticationResponse.Granted
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.ClientFactory
import com.bitmovin.analytics.utils.DataSerializer.deserialize
import com.bitmovin.analytics.utils.DataSerializer.serialize
import com.bitmovin.analytics.utils.HttpClient
import com.bitmovin.analytics.utils.Util
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

internal class DefaultLicenseCall(
    config: AnalyticsConfig,
    private val licenseKeyProvider: LicenseKeyProvider,
    private val context: Context,
    private val httpClient: HttpClient =
        HttpClient(
            context,
            ClientFactory().createClient(config),
        ),
    private val backendUrl: String =
        Uri.parse(config.backendUrl)
            .buildUpon()
            .appendEncodedPath("licensing")
            .build()
            .toString(),
) : LicenseCall {
    init {
        BitmovinLog.d(TAG, String.format("Initialized license call with backendUrl: %s", backendUrl))
    }

    override suspend fun authenticate(callback: AuthenticationCallback) {
        when (val result = licenseKeyProvider.waitForResult()) {
            is LicenseKeyState.Provided -> {
                callLicenseBackend(result.licenseKey, callback)
            }

            is LicenseKeyState.Timeout -> {
                BitmovinLog.d(TAG, "License call failed due to license key not being provided in time")
                callback.authenticationCompleted(Error)
            }

            is LicenseKeyState.NotProvided -> {
                BitmovinLog.d(TAG, "License call failed due to license key not being provided")
                callback.authenticationCompleted(Error)
            }
        }
    }

    private fun callLicenseBackend(
        licenseKey: String,
        callback: AuthenticationCallback,
    ) {
        val data = LicenseCallData(licenseKey, Util.analyticsVersion, Util.getDomain(context))
        httpClient.post(
            backendUrl,
            serialize(data),
            LicenseRequestCallback(licenseKey, callback),
        )
    }
}

private class LicenseRequestCallback(
    private val licenseKey: String,
    private val callback: AuthenticationCallback,
) : Callback {
    override fun onFailure(
        call: Call,
        e: IOException,
    ) {
        BitmovinLog.e(TAG, "License call failed due to connectivity issues", e)
        callback.authenticationCompleted(Error)
    }

    @Throws(IOException::class)
    override fun onResponse(
        call: Call,
        response: Response,
    ) {
        val body = response.body

        if (body == null) {
            BitmovinLog.d(TAG, "License call was denied without providing a response body")
            callback.authenticationCompleted(Error)
            return
        }

        val licenseResponse = deserialize(body.string(), LicenseResponse::class.java)
        if (licenseResponse == null) {
            BitmovinLog.d(TAG, "License call was denied without providing a valid response body")
            callback.authenticationCompleted(Error)
            return
        }
        if (licenseResponse.status == null) {
            BitmovinLog.d(TAG, "License response was denied without status")
            callback.authenticationCompleted(Error)
            return
        }
        if (licenseResponse.status != "granted") {
            BitmovinLog.d(TAG, "License response was denied: ${licenseResponse.message}")
            callback.authenticationCompleted(Denied(licenseResponse.message))
            return
        }
        BitmovinLog.d(TAG, "License response was granted")
        callback.authenticationCompleted(Granted(licenseKey, licenseResponse.features))
    }
}

private const val TAG = "LicenseCall"
