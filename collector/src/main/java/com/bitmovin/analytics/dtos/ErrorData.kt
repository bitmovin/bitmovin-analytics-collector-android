package com.bitmovin.analytics.dtos

import com.bitmovin.analytics.utils.topOfStacktrace
import kotlinx.serialization.Serializable

@Serializable
data class ErrorData(
    val exceptionMessage: String? = null,
    val exceptionStacktrace: Collection<String>? = null,
    val additionalData: String? = null,
) {
    companion object {
        fun fromThrowable(
            throwable: Throwable,
            additionalData: String? = null,
        ): ErrorData = ErrorData(throwable.message ?: throwable.toString(), throwable.topOfStacktrace, additionalData)
    }
}
