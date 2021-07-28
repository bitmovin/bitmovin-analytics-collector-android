package com.bitmovin.analytics.data

data class ErrorCode(val errorCode: Int, val description: String, val errorData: com.bitmovin.analytics.features.errordetails.ErrorData, val legacyErrorData: ErrorData? = null) {

    @Override
    override fun toString(): String {
        return "$errorCode: $description"
    }
}
