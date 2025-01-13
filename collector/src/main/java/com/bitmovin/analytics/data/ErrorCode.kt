package com.bitmovin.analytics.data

import com.bitmovin.analytics.features.errordetails.ErrorData

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class ErrorCode(val errorCode: Int, val description: String, val errorData: ErrorData, val legacyErrorData: LegacyErrorData? = null) {
    @Override
    override fun toString(): String {
        return "$errorCode: $description"
    }
}
