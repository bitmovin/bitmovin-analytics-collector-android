package com.bitmovin.analytics.utils

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test

class DownloadSpeedMeterTest {
    private var measurement1 = DownloadSpeedMeasurement(20, 1000)
    private var measurement2 = DownloadSpeedMeasurement(30, 1500)
    private var measurement3 = DownloadSpeedMeasurement(50, 2000)
    private var measurement4 = DownloadSpeedMeasurement(20, 2000)
    private var measurement5 = DownloadSpeedMeasurement(20, 1500)
    private var meter = DownloadSpeedMeter()

    @Before
    fun setup() {
        meter.reset()
    }

    @Test
    fun testMeterAddAndRest() {
        meter.addMeasurement(measurement1)
        meter.addMeasurement(measurement2)
        meter.addMeasurement(measurement3)
        meter.addMeasurement(measurement4)
        meter.addMeasurement(measurement5)

        var info = meter.getInfoAndReset()

        Assertions.assertThat(info.segmentsDownloadCount).isEqualTo(5)

        info = meter.getInfoAndReset()

        Assertions.assertThat(info.segmentsDownloadCount).isEqualTo(0)
    }

    @Test
    fun testSpeedMeasurements() {
        meter.addMeasurement(measurement1)
        meter.addMeasurement(measurement2)
        meter.addMeasurement(measurement3)
        meter.addMeasurement(measurement4)
        meter.addMeasurement(measurement5)

        val info = meter.getInfoAndReset()
        // total of 5 measurements
        Assertions.assertThat(info.segmentsDownloadCount).isEqualTo(5)
        // sum of all durations
        Assertions.assertThat(info.segmentsDownloadTime).isEqualTo(140)
        // sum of all sizes
        Assertions.assertThat(info.segmentsDownloadSize).isEqualTo(8000)
        // slowest download -> measurement4
        Assertions.assertThat(info.minDownloadSpeed).isEqualTo(320.0f)
        // fastest download -> measurement3
        Assertions.assertThat(info.maxDownloadSpeed).isEqualTo(800.0f)
        Assertions.assertThat(info.avgDownloadSpeed).isEqualTo(504.0f)
    }

    @Test
    fun `reset clears the value after calling`() {
        meter.addMeasurement(measurement1)
        meter.addMeasurement(measurement2)
        meter.addMeasurement(measurement3)
        meter.addMeasurement(measurement4)
        meter.addMeasurement(measurement5)

        meter.reset()

        val info = meter.getInfoAndReset()

        Assertions.assertThat(info.segmentsDownloadCount).isEqualTo(0)
    }
}
