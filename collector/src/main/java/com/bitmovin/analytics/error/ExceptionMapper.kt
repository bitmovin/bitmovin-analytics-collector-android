package com.bitmovin.analytics.error

import com.bitmovin.analytics.data.ErrorCode

interface ExceptionMapper<in T> {
    fun map(throwable: T): ErrorCode
}
