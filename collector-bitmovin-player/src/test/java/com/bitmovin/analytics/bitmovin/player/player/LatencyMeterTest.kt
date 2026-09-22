package com.bitmovin.analytics.bitmovin.player.player

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class LatencyMeterTest {
    private val meter = LatencyMeter()

    @Before
    fun setup() {
        meter.reset()
    }

    @Test
    fun `getInfoAndReset returns null given no measurements`() {
        assertThat(meter.getInfoAndReset()).isNull()
    }

    @Test
    fun `getInfoAndReset averages latencies and converts to milliseconds`() {
        meter.addMeasurement(2.0, 3.0)
        meter.addMeasurement(3.0, 3.0)
        meter.addMeasurement(4.0, 3.0)

        val info = meter.getInfoAndReset()

        assertThat(info).isNotNull
        assertThat(info!!.avgLatency).isEqualTo(3000L)
        assertThat(info.targetLatency).isEqualTo(3000L)
        assertThat(info.avgLatencyDelta).isEqualTo(0L)
    }

    @Test
    fun `getInfoAndReset reports positive delta given playback behind target`() {
        meter.addMeasurement(4.5, 3.0)

        val info = meter.getInfoAndReset()

        assertThat(info!!.avgLatency).isEqualTo(4500L)
        assertThat(info.avgLatencyDelta).isEqualTo(1500L)
    }

    @Test
    fun `getInfoAndReset reports negative delta given playback ahead of target`() {
        meter.addMeasurement(2.0, 3.5)

        val info = meter.getInfoAndReset()

        assertThat(info!!.avgLatency).isEqualTo(2000L)
        assertThat(info.avgLatencyDelta).isEqualTo(-1500L)
    }

    @Test
    fun `getInfoAndReset leaves target and delta null given no target latency`() {
        meter.addMeasurement(4.0, null)

        val info = meter.getInfoAndReset()

        assertThat(info!!.avgLatency).isEqualTo(4000L)
        assertThat(info.targetLatency).isNull()
        assertThat(info.avgLatencyDelta).isNull()
    }

    @Test
    fun `getInfoAndReset averages the delta per measurement given target changes in flight`() {
        // 1s behind the target, then 1s ahead of the new target
        meter.addMeasurement(4.0, 3.0)
        meter.addMeasurement(4.0, 5.0)

        val info = meter.getInfoAndReset()

        assertThat(info!!.avgLatency).isEqualTo(4000L)
        assertThat(info.targetLatency).isEqualTo(5000L)
        assertThat(info.avgLatencyDelta).isEqualTo(0L)
    }

    @Test
    fun `getInfoAndReset only uses measurements with a target for the delta`() {
        meter.addMeasurement(4.0, null)
        meter.addMeasurement(5.0, 3.0)

        val info = meter.getInfoAndReset()

        assertThat(info!!.avgLatency).isEqualTo(4500L)
        assertThat(info.targetLatency).isEqualTo(3000L)
        assertThat(info.avgLatencyDelta).isEqualTo(2000L)
    }

    @Test
    fun `getInfoAndReset starts a new interval`() {
        meter.addMeasurement(4.0, 3.0)
        meter.getInfoAndReset()

        assertThat(meter.getInfoAndReset()).isNull()

        meter.addMeasurement(6.0, null)
        val info = meter.getInfoAndReset()
        assertThat(info!!.avgLatency).isEqualTo(6000L)
        assertThat(info.targetLatency).isNull()
    }

    @Test
    fun `addMeasurement ignores negative and non finite latencies`() {
        meter.addMeasurement(-1.0, 3.0)
        meter.addMeasurement(Double.NaN, 3.0)
        meter.addMeasurement(Double.POSITIVE_INFINITY, 3.0)

        assertThat(meter.getInfoAndReset()).isNull()
    }
}
