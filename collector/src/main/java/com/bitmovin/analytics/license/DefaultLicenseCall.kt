package com.bitmovin.analytics.license

import android.content.Context
import android.net.Uri
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.dtos.LicenseCallData
import com.bitmovin.analytics.dtos.LicenseResponse
import com.bitmovin.analytics.license.AuthenticationResponse.Denied
import com.bitmovin.analytics.license.AuthenticationResponse.Error
import com.bitmovin.analytics.license.AuthenticationResponse.Granted
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.ClientFactory
import com.bitmovin.analytics.utils.DataSerializerKotlinX.deserialize
import com.bitmovin.analytics.utils.DataSerializerKotlinX.serialize
import com.bitmovin.analytics.utils.HttpClient
import com.bitmovin.analytics.utils.Util
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

internal class DefaultLicenseCall(
    config: AnalyticsConfig,
    private val licenseKeyProvider: LicenseKeyProvider,
    private val context: Context,
    private val httpClient: HttpClient =
        HttpClient(
            context,
            ClientFactory().createLicenseClient(config),
        ),
    private val backendUrl: String =
        Uri
            .parse(config.backendUrl)
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
        val requestCallback = LicenseRequestCallback(licenseKey, callback)
        try {
            val data = LicenseCallData(licenseKey, Util.analyticsVersion, Util.getDomain(context))
            httpClient.post(backendUrl, serialize(data), requestCallback)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "License call could not be sent", e)
            requestCallback.complete(Error)
        }
    }
}

private class LicenseRequestCallback(
    private val licenseKey: String,
    private val callback: AuthenticationCallback,
) : Callback {
    private val completed = AtomicBoolean(false)

    fun complete(response: AuthenticationResponse) {
        if (completed.compareAndSet(false, true)) {
            callback.authenticationCompleted(response)
        }
    }

    override fun onFailure(
        call: Call,
        e: IOException,
    ) {
        BitmovinLog.e(TAG, "License call failed due to connectivity issues", e)
        complete(Error)
    }

    override fun onResponse(
        call: Call,
        response: Response,
    ) {
        val result =
            try {
                parse(response)
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "License response could not be read", e)
                Error
            }
        complete(result)
    }

    private fun parse(response: Response): AuthenticationResponse {
        val body = response.body

        if (body == null) {
            BitmovinLog.d(TAG, "License call was denied without providing a response body")
            return Error
        }

        val licenseResponse = deserialize(body.string(), LicenseResponse::class.java)
        if (licenseResponse == null) {
            BitmovinLog.d(TAG, "License call was denied without providing a valid response body")
            return Error
        }
        if (licenseResponse.status == null) {
            BitmovinLog.d(TAG, "License response was denied without status")
            return Error
        }
        if (licenseResponse.status != "granted") {
            BitmovinLog.d(TAG, "License response was denied: ${licenseResponse.message}")
            return Denied(licenseResponse.message)
        }
        BitmovinLog.d(TAG, "License response was granted")
        return Granted(licenseKey, licenseResponse.features)
    }
}

private const val TAG = "LicenseCall"
