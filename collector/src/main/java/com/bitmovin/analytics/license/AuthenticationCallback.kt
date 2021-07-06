package com.bitmovin.analytics.license

interface AuthenticationCallback {
    fun authenticationCompleted(success: Boolean, features: Map<String, String>?)
}
