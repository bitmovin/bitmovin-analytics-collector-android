package com.bitmovin.analytics.utils

import com.bitmovin.analytics.data.DownloadSpeedInfo

const val THRESHOLD_BYTES_PER_MS = 1_500_000_000 / 8 / 1000; // 1.5 Gigabit per second in bytes per millisecond (1.5^9 / 8 / 1000)

class DownloadSpeedMeter {
    private var measurements = ArrayList<DownloadSpeedMeasurement>()

    @Synchronized
    fun reset() {
        measurements.clear()
    }

    /**
     * Add a new speed measurement to the meter.
     */
    @Synchronized
    fun addMeasurement(measurement: DownloadSpeedMeasurement) {
        if (measurement.httpStatusCode != null && measurement.httpStatusCode >= 300) {
            return
        }

        if (measurement.durationInMs <= 0L || measurement.downloadSizeInBytes <= 0) {
            return
        }

        if (measurement.speedInBytesPerMs >= THRESHOLD_BYTES_PER_MS || measurement.speedInBytesPerMs <= 0) {
            return
        }

        measurements.add(measurement)
    }

    /**
     * Get the download speed information and resets it afterwards.
     *
     * Should only included in the event data if the player supports it.
     */
    @Synchronized
    fun getInfoAndReset(): DownloadSpeedInfo {
        val downloadInfos =
            DownloadSpeedInfo(
                segmentsDownloadCount = measurements.size,
                segmentsDownloadSize = measurements.sumOf { it.downloadSizeInBytes },
                segmentsDownloadTime = totalTimeInMs(),
                avgDownloadSpeed = avgSpeedInKbps(),
                minDownloadSpeed = minSpeedInKbps(),
                maxDownloadSpeed = maxSpeedInKbps(),
                avgTimeToFirstByte = avgTimeToFirstByteInMs(),
            )
        reset()
        return downloadInfos
    }

    private fun avgSpeedInKbps(): Float? {
        if (measurements.isEmpty()) {
            return null
        }
        return measurements.map { it.speedInBytesPerMs }.average().times(8).toFloat(); // bytes per millisecond to kbps
    }

    private fun minSpeedInKbps(): Float? {
        // the slowest one to download
        return measurements.minOfOrNull { it.speedInBytesPerMs }?.times(8) // bytes per millisecond to kbps
    }

    private fun maxSpeedInKbps(): Float? {
        // the fastest one to download
        return measurements.maxOfOrNull { it.speedInBytesPerMs }?.times(8); // bytes per millisecond to kbps
    }

    private fun totalTimeInMs(): Long {
        if (this.measurements.isEmpty()) {
            return 0
        }
        return this.measurements.sumOf { it.durationInMs }
    }

    private fun avgTimeToFirstByteInMs(): Float? {
        val nonNullTimeToFirstByte = measurements.filter { it.timeToFirstByteInMs != null }.map { it.timeToFirstByteInMs!! }
        if (nonNullTimeToFirstByte.isEmpty()) {
            return null
        }

        return nonNullTimeToFirstByte.average().toFloat()
    }
}
