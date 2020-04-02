package com.bitmovin.analytics.error

import com.bitmovin.analytics.data.ErrorCode

interface ExceptionMapper<in T : Throwable> {
    fun map(throwable: T): ErrorCode
}
