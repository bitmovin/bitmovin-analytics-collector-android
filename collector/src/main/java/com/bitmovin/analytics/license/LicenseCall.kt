package com.bitmovin.analytics.license

internal interface LicenseCall {
    suspend fun authenticate(callback: AuthenticationCallback)
}
