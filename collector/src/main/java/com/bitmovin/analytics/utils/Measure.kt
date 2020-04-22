package com.bitmovin.analytics.utils;

import com.bitmovin.analytics.data.SpeedMeasurement

class Measure(private val download: SpeedMeasurement) {

    fun getSpeed(): Float {
        return download.size.toFloat().div(download.duration) //bytes per millisecond
    }

    fun getDuration(): Long {
        return download.duration
    }

    fun getSize(): Long {
        return download.size
    }

    fun getTimeToFirstByte(): Float {
        return download.timeToFirstByte
    }

    fun getHttpStatus(): Int {
        return download.httpStatus;
    }
}