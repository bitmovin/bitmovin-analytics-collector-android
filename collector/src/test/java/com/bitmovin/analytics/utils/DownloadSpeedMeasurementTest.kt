package com.bitmovin.analytics.utils

import org.assertj.core.api.Assertions
import org.junit.Test

class DownloadSpeedMeasurementTest {
    @Test
    fun `speed measurement should handle 0 and infinity`() {
        val measurement = DownloadSpeedMeasurement(0, 0)
        Assertions.assertThat(measurement.speedInBytesPerMs).isEqualTo(0f)

        val measurement2 = DownloadSpeedMeasurement(0, 100)
        Assertions.assertThat(measurement2.speedInBytesPerMs).isEqualTo(0f)

        val measurement3 = DownloadSpeedMeasurement(100, 0)
        Assertions.assertThat(measurement3.speedInBytesPerMs).isEqualTo(0f)

        val measurement4 = DownloadSpeedMeasurement(100, 100)
        Assertions.assertThat(measurement4.speedInBytesPerMs).isEqualTo(1f)

        val measurement5 = DownloadSpeedMeasurement(100, 0)
        Assertions.assertThat(measurement5.speedInBytesPerMs).isEqualTo(0f)
    }
}
