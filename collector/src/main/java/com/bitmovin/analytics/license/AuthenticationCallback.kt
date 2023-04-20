package com.bitmovin.analytics.license

interface AuthenticationCallback {
    fun authenticationCompleted(success: Boolean, featureConfigs: FeatureConfigContainer?)
}

sealed class AuthenticationResponse {
    data class Granted(
        val featureConfigContainer: FeatureConfigContainer?
    ): AuthenticationResponse()

    data class Denied(
        val message: String
    ): AuthenticationResponse()

    object Error : AuthenticationResponse()
}