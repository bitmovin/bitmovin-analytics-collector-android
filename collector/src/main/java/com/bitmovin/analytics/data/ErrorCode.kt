package com.bitmovin.analytics.data

class ErrorCode(val errorCode: Int, var description: String, val errorData: com.bitmovin.analytics.features.errordetails.ErrorData, var legacyErrorData: ErrorData? = null) {

    @Override
    override fun toString(): String {
        return "$errorCode: $description"
    }
}
