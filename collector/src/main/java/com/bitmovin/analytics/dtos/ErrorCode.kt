package com.bitmovin.analytics.dtos

import kotlinx.serialization.Serializable

@Serializable
data class ErrorCode(val errorCode: Int, val description: String, val errorData: ErrorData, val legacyErrorData: LegacyErrorData? = null) {
    @Override
    override fun toString(): String {
        return "$errorCode: $description"
    }
}
