package com.bitmovin.analytics.license

import android.content.Context
import android.net.Uri
import android.util.Log
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.data.LicenseCallData
import com.bitmovin.analytics.data.LicenseResponse
import com.bitmovin.analytics.utils.ClientFactory
import com.bitmovin.analytics.utils.DataSerializer.deserialize
import com.bitmovin.analytics.utils.DataSerializer.serialize
import com.bitmovin.analytics.utils.HttpClient
import com.bitmovin.analytics.utils.Util
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class LicenseCall(private val config: BitmovinAnalyticsConfig, private val context: Context) {
    private val backendUrl: String
    private val httpClient: HttpClient

    init {
        backendUrl = Uri.parse(config.config.backendUrl)
            .buildUpon()
            .appendEncodedPath("licensing")
            .build()
            .toString()

        Log.d(TAG, String.format("Initialized License Call with backendUrl: %s", backendUrl))
        httpClient = HttpClient(
            context,
            ClientFactory().createClient(
                config.config,
            ),
        )
    }

    fun authenticate(callback: AuthenticationCallback) {
        val data = LicenseCallData(config.key, Util.analyticsVersion, Util.getDomain(context))
        val json = serialize(data)
        httpClient.post(
            backendUrl,
            json,
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "License call failed due to connectivity issues", e)
                    callback.authenticationCompleted(AuthenticationResponse.Error)
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body

                    if (body == null) {
                        Log.d(TAG, "License call was denied without providing a response body")
                        callback.authenticationCompleted(AuthenticationResponse.Error)
                        return
                    }

                    val licenseResponse = deserialize(
                        body.string(),
                        LicenseResponse::class.java,
                    )
                    if (licenseResponse == null) {
                        Log.d(TAG, "License call was denied without providing a response body")
                        callback.authenticationCompleted(AuthenticationResponse.Error)
                        return
                    }
                    if (licenseResponse.status == null) {
                        Log.d(TAG, String.format("License response was denied without status"))
                        callback.authenticationCompleted(AuthenticationResponse.Error)
                        return
                    }
                    if (licenseResponse.status != "granted") {
                        Log.d(
                            TAG,
                            String.format(
                                "License response was denied: %s",
                                licenseResponse.message,
                            ),
                        )
                        callback.authenticationCompleted(
                            AuthenticationResponse.Denied(licenseResponse.message)
                        )
                        return
                    }
                    Log.d(TAG, "License response was granted")
                    callback.authenticationCompleted(
                        AuthenticationResponse.Granted(licenseResponse.features)
                    )
                }
            },
        )
    }

    companion object {
        private const val TAG = "LicenseCall"
    }
}
