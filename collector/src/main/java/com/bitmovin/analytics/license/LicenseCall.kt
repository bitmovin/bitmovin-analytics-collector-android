package com.bitmovin.analytics.license

internal interface LicenseCall {
    fun authenticate(callback: AuthenticationCallback)
}
