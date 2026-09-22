package com.bitmovin.analytics.dtos

import kotlinx.serialization.Serializable

/**
 * Live latency information of one playing sample interval. All values are in milliseconds.
 */
@Serializable
data class LatencyInfo(
    /**
     * Average distance from the live edge, within the current sample duration
     */
    val avgLatency: Long,
    /**
     * Latency the player was configured to maintain, as of the end of the interval.
     *
     * null when the source has no target latency configured.
     */
    val targetLatency: Long? = null,
    /**
     * Average distance from the target, positive when playback ran behind the target
     * and negative when it ran ahead of it. The distance is measured against the target that was
     * in effect at the time of each measurement, so it stays meaningful when the target changes
     * within the interval (and then differs from `avgLatency - targetLatency`).
     *
     * null when there is no target.
     */
    val avgLatencyDelta: Long? = null,
)
