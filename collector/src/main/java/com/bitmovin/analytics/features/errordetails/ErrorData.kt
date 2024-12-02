package com.bitmovin.analytics.features.errordetails

import androidx.annotation.Keep
import com.bitmovin.analytics.utils.topOfStacktrace

@Keep // Protect from obfuscation in case customers are using proguard
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
