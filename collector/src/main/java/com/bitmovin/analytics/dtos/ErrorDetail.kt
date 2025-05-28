package com.bitmovin.analytics.dtos

import com.bitmovin.analytics.utils.Util

// DTO which is protected from ProGuard obfuscation through proguard-consumer-rules.pro
data class ErrorDetail(
    val platform: String,
    val licenseKey: String?,
    val domain: String,
    val impressionId: String,
    val errorId: Long,
    val timestamp: Long,
    val code: Int?,
    val message: String?,
    val data: ErrorData,
    val httpRequests: List<HttpRequest>?,
    val analyticsVersion: String = Util.analyticsVersion,
)
