package com.bitmovin.analytics.data

class ErrorCode(val errorCode: Int, var description: String, var legacyErrorData: ErrorData? = null) {

    @Override
    override fun toString(): String {
        return "$errorCode: $description"
    }
}
