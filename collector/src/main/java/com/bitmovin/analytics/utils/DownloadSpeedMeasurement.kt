package com.bitmovin.analytics.utils

import java.util.Date

class DownloadSpeedMeasurement(
    val durationInMs: Long,
    val downloadSizeInBytes: Long,
    val timeToFirstByteInMs: Long? = null,
    val timestamp: Date = Date(),
    val httpStatusCode: Int? = null,
) {
    val speedInBytesPerMs: Float
        get() = if (durationInMs > 0) downloadSizeInBytes.toFloat().div(durationInMs) else 0f // bytes per millisecond
}
