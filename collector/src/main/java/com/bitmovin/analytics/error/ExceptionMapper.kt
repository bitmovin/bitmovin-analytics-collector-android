package com.bitmovin.analytics.error

import com.bitmovin.analytics.dtos.ErrorCode

interface ExceptionMapper<in T> {
    fun map(throwable: T): ErrorCode
}
