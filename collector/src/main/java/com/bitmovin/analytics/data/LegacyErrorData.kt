package com.bitmovin.analytics.data

import androidx.annotation.Keep

// LegacyErrorData is stored unindexed as error_data in crate
@Keep // Protect from obfuscation in case customers are using proguard
data class LegacyErrorData(val msg: String, val details: Array<String> = emptyArray())
