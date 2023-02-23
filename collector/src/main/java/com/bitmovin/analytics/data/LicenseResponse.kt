package com.bitmovin.analytics.data

import androidx.annotation.Keep
import com.bitmovin.analytics.license.FeatureConfigContainer

@Keep // Protect from obfuscation in case customers are using proguard
data class LicenseResponse(val status: String?, val message: String?, val features: FeatureConfigContainer?)
