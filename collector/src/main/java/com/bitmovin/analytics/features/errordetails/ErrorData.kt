package com.bitmovin.analytics.features.errordetails

data class ErrorData(val exceptionMessage: String?, val exceptionStacktrace: Collection<String>?, val additionalData: String?)
