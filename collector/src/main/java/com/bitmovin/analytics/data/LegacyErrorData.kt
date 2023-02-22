package com.bitmovin.analytics.data

import androidx.annotation.Keep

@Keep // Protect from obfuscation in case customers are using proguard
data class LegacyErrorData(val msg: String, val details: Array<String> = emptyArray())
