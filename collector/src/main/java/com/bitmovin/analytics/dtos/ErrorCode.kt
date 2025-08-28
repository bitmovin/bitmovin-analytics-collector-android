package com.bitmovin.analytics.dtos

import com.bitmovin.analytics.api.error.ErrorSeverity
import kotlinx.serialization.Serializable

@Serializable
data class ErrorCode(
    val errorCode: Int,
    val message: String,
    val errorData: ErrorData,
    // legacyErrorData is stored within crate as error_data
    val legacyErrorData: LegacyErrorData? = null,
    val errorSeverity: ErrorSeverity = ErrorSeverity.CRITICAL,
) {
    @Override
    override fun toString(): String {
        return "$errorCode: $message (Severity: $errorSeverity)"
    }
}
