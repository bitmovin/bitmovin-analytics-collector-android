package com.bitmovin.analytics.utils

import com.bitmovin.analytics.data.DownloadSpeedInfo
import com.bitmovin.analytics.data.SpeedMeasurement

class DownloadSpeedMeter {

    private var measures = ArrayList<Measure>()
    private val thresholdBytes = 37500; // 300 Megabit per second in bytes per millisecond

    fun reset() {
        measures.clear()
    }

    fun addMeasurement(measurement: SpeedMeasurement) {
        if (measurement.httpStatus >= 400) {
            return
        }

        val measure = Measure(measurement)

        if (measure.speed >= thresholdBytes) {
            return
        }

        measures.add(measure)
    }

    fun getInfo(): DownloadSpeedInfo {
        val info = DownloadSpeedInfo()
        info.segmentsDownloadCount = measures.size
        info.segmentsDownloadSize = measures.map { it.downloadSize }.sum()
        info.segmentsDownloadTime = totalTime()
        info.avgDownloadSpeed = avgSpeed()
        info.minDownloadSpeed = minSpeed()
        info.maxDownloadSpeed = maxSpeed()
        info.avgTimeToFirstByte = avgTimeToFirstByte()
        return info
    }

    private fun avgSpeed(): Float {
        if (measures.isEmpty()) {
            return 0.0f
        }
        val totalSpeed = measures.map { it.speed }.sum()

        return totalSpeed.div(measures.size).times(8); // bytes per millisecond to kbps
    }

    private fun minSpeed(): Float? {
        if (measures.isEmpty()) {
            return 0.0f
        }
        // the slowest one to download
        return measures.map { it.speed }.max()?.times(8) // bytes per millisecond to kbps
    }

    private fun maxSpeed(): Float? {
        if (measures.isEmpty()) {
            return 0.0f
        }
        // the fastest one to download
        return measures.map { it.speed }.min()?.times(8); // bytes per millisecond to kbps
    }

    private fun totalTime(): Long {
        if (this.measures.isEmpty()) {
            return 0
        }
        return this.measures.map { it.duration }.sum()
    }

    private fun avgTimeToFirstByte(): Float {
        if (measures.isEmpty()) {
            return 0.0f
        }

        return measures.map { it.timeToFirstByte }.sum().div(measures.size)
    }
}
