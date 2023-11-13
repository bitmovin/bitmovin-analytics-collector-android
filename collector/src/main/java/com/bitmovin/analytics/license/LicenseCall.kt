package com.bitmovin.analytics.license

internal interface LicenseCall {
    fun authenticate(licenseKey: String, callback: AuthenticationCallback)
}
