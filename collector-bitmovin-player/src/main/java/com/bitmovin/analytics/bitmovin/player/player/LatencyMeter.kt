package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.analytics.dtos.LatencyInfo
import com.bitmovin.analytics.utils.Util

/**
 * Collects live latency measurements of the player over one sample interval and
 * aggregates them into a [LatencyInfo]. All inputs are in seconds (player units),
 * the output is in milliseconds (analytics units).
 *
 * The average is aggregated on the fly as a running sum and count. This assumes that the
 * measurements arrive at a roughly fixed rate (the player's time update events), so every
 * measurement carries the same weight.
 *
 * The meter does not know whether a measurement is meaningful (e.g. the player plays at its target
 * latency and not time shifted into the DVR window), the caller only feeds it with measurements that are.
 * An interval without measurements yields no [LatencyInfo] at all.
 */
internal class LatencyMeter {
    private var latencySumInSeconds = 0.0
    private var latencyCount = 0

    // distance to the target, accumulated per measurement so that a target change within the
    // interval is reflected correctly; only measurements with a target contribute
    private var latencyDeltaSumInSeconds = 0.0
    private var latencyDeltaCount = 0

    // most recent target latency of the interval
    private var lastTargetLatencyInSeconds: Double? = null

    @Synchronized
    fun reset() {
        latencySumInSeconds = 0.0
        latencyCount = 0
        latencyDeltaSumInSeconds = 0.0
        latencyDeltaCount = 0
        lastTargetLatencyInSeconds = null
    }

    /**
     * Adds a latency measurement. Negative or non-finite latencies are dropped, since the player
     * reports -1 when the latency is unknown or the source is not live.
     * The distance to [targetLatencyInSeconds] is recorded with every measurement, so the reported
     * delta stays correct even when the target changes within the interval.
     */
    @Synchronized
    fun addMeasurement(
        latencyInSeconds: Double,
        targetLatencyInSeconds: Double?,
    ) {
        if (!latencyInSeconds.isFinite() || latencyInSeconds < 0) {
            return
        }

        latencySumInSeconds += latencyInSeconds
        latencyCount++

        if (targetLatencyInSeconds != null) {
            latencyDeltaSumInSeconds += latencyInSeconds - targetLatencyInSeconds
            latencyDeltaCount++
            lastTargetLatencyInSeconds = targetLatencyInSeconds
        }
    }

    /**
     * Aggregates the measurements of the current interval and starts a new one.
     * Returns null when no latency was recorded.
     */
    @Synchronized
    fun getInfoAndReset(): LatencyInfo? {
        if (latencyCount == 0) {
            reset()
            return null
        }

        val latencyInfo =
            LatencyInfo(
                avgLatency = Util.secondsToMillis(latencySumInSeconds / latencyCount),
                targetLatency = lastTargetLatencyInSeconds?.let { Util.secondsToMillis(it) },
                avgLatencyDelta =
                    if (latencyDeltaCount > 0) {
                        Util.secondsToMillis(latencyDeltaSumInSeconds / latencyDeltaCount)
                    } else {
                        null
                    },
            )
        reset()
        return latencyInfo
    }
}
