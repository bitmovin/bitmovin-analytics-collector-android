package com.bitmovin.analytics.features.errordetails

import androidx.annotation.Keep
import com.bitmovin.analytics.features.httprequesttracking.HttpRequest
import com.bitmovin.analytics.utils.Util

@Keep // Protect from obfuscation in case customers are using proguard
data class ErrorDetail(val platform: String, val licenseKey: String, val domain: String, val impressionId: String, val errorId: Long, val timestamp: Long, val code: Int?, val message: String?, val data: ErrorData, val httpRequests: List<HttpRequest>?, val analyticsVersion: String = Util.analyticsVersion)
