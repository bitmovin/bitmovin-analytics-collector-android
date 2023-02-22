package com.bitmovin.analytics.license

import androidx.annotation.Keep
import com.bitmovin.analytics.features.errordetails.ErrorDetailTrackingConfig

@Keep // Protect from obfuscation in case customers are using proguard
data class FeatureConfigContainer(val errorDetails: ErrorDetailTrackingConfig?)
