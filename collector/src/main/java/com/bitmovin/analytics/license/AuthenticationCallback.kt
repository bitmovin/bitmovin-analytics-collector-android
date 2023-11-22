package com.bitmovin.analytics.license

fun interface AuthenticationCallback {
    fun authenticationCompleted(response: AuthenticationResponse)
}

sealed class AuthenticationResponse {
    data class Granted(
        val licenseKey: String,
        val featureConfigContainer: FeatureConfigContainer?,
    ) : AuthenticationResponse()

    data class Denied(
        val message: String?,
    ) : AuthenticationResponse()

    object Error : AuthenticationResponse()
}
