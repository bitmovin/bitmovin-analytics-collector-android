package com.bitmovin.analytics.utils;

import com.bitmovin.analytics.data.SpeedMeasurement

class Measure(private val download: SpeedMeasurement) {

    var speed: Float = 0.0f
        get() {
            return download.size.toFloat().div(download.duration) //bytes per millisecond
        }

    var duration: Long = 0
        get() {
            return download.duration
        }

    var downloadSize: Long = 0
        get() {
            return download.size
        }

    var timeToFirstByte: Float = 0.0f
        get() {
            return download.timeToFirstByte
        }

    var httpStatus: Int = 0
        get() {
            return download.httpStatus;
        }
}
