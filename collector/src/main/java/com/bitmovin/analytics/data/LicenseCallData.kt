package com.bitmovin.analytics.data

import androidx.annotation.Keep

@Keep // Protect from obfuscation in case customers are using proguard
data class LicenseCallData(val key: String, val analyticsVersion: String, val domain: String)
