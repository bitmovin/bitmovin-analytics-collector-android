package com.bitmovin.analytics.license

interface AuthenticationCallback {
    fun authenticationCompleted(success: Boolean, settings: Map<String, String>?)
}
