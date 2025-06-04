package com.bitmovin.analytics.dtos

import kotlinx.serialization.Serializable

@Serializable
data class HttpRequest(
    val timestamp: Long,
    val type: String,
    val url: String?,
    val lastRedirectLocation: String?,
    val httpStatus: Int,
    /**
     * Total time elapsed since the request was opened (including TTFB).
     */
    val downloadTime: Long,
    val timeToFirstByte: Long?,
    val size: Long?,
    val success: Boolean,
)
