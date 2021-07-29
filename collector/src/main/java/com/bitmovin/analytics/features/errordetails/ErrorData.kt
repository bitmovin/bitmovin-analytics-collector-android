package com.bitmovin.analytics.features.errordetails

data class ErrorData(val exceptionMessage: String? = null, val exceptionStacktrace: Collection<String>? = null, val additionalData: String? = null)
