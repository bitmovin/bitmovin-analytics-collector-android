package com.bitmovin.analytics.features.errordetails

import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.dtos.ErrorData

interface OnErrorDetailEventListener {
    fun onError(
        impressionId: String,
        code: Int?,
        message: String?,
        errorData: ErrorData?,
        errorSeverity: ErrorSeverity,
    )
}
