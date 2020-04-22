package com.bitmovin.analytics.data;

import java.util.*

class SpeedMeasurement() {

    // Download time in milliseconds
    var duration: Long = 0

    // Bytes downloaded
    var size: Long = 0;

    //Time to first byte of this download
    var timeToFirstByte: Float = 0.0f;

    // Time the download finished
    lateinit var timestamp: Date;

    // HTTP Status of the download measurement
    var httpStatus: Int = 0;

    constructor(duration: Long, size: Long) : this() {
        this.duration = duration;
        this.size = size;
    }
}