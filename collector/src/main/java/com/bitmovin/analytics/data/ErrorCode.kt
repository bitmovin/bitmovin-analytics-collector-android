package com.bitmovin.analytics.data

import androidx.annotation.Keep
import com.bitmovin.analytics.features.errordetails.ErrorData

@Keep // Protect from obfuscation in case customers are using proguard
data class ErrorCode(val errorCode: Int, val description: String, val errorData: ErrorData, val legacyErrorData: LegacyErrorData? = null) {

    @Override
    override fun toString(): String {
        return "$errorCode: $description"
    }
}
