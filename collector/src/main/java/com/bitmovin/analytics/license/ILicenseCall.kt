package com.bitmovin.analytics.license

internal interface ILicenseCall {
    fun authenticate(callback: AuthenticationCallback)
}
