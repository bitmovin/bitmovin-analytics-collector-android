package com.bitmovin.analytics.bitmovin.player.player

import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player
import kotlin.math.abs

/**
 * Feeds the [LatencyMeter] with live latency measurements.
 *
 * A measurement is only meaningful while the player actually plays at its target latency. Whether that is
 * the case is decided by comparing the target latency with the player's effective target playback latency
 * (see [isPlayingAtTargetLatency]), which the player only provides asynchronously via a callback. On every time update a request
 * is issued (at most one in flight), and the measurement is taken in the callback, so the latency reading
 * and the target it is judged against belong to the same moment. Nothing ever blocks the calling thread.
 *
 * Players without the target playback latency API get no latency tracking at all.
 */
internal class LiveLatencyTracker(
    private val player: Player,
    private val latencyMeter: LatencyMeter,
    private val elapsedTimeProvider: () -> Long = { Util.elapsedTime },
) {
    // elapsed time at which the pending request was issued, null when no request is in flight
    private var requestInFlightSince: Long? = null
    private var isSupported = true
    private var isReleased = false

    /**
     * Requests the effective target playback latency from the player. The measurement is taken once
     * the player answers, see [onTargetPlaybackLatency].
     */
    fun onTimeChanged() {
        if (isReleased || !isSupported || !player.isLive || isRequestInFlight()) {
            return
        }

        requestInFlightSince = elapsedTimeProvider()
        val requested = player.requestTargetPlaybackLatency(::onTargetPlaybackLatency)
        if (!requested) {
            requestInFlightSince = null
            isSupported = false
            BitmovinLog.d(TAG, "Player does not support the target playback latency API, live latency tracking is disabled")
        }
    }

    fun resetSourceRelatedState() {
        // a pending request of the previous source may never be answered
        requestInFlightSince = null
    }

    fun release() {
        isReleased = true
        requestInFlightSince = null
    }

    // No synchronization needed: the player answers on the main thread (it launches the request in a main
    // dispatcher scope), which is also the thread of the player events and of the collector's state machine.
    private fun onTargetPlaybackLatency(targetPlaybackLatencyInSeconds: Double?) {
        requestInFlightSince = null
        if (isReleased) {
            return
        }

        try {
            val targetLatencyInSeconds = player.targetLatencyInSecondsOrNull()
            if (!isPlayingAtTargetLatency(targetPlaybackLatencyInSeconds, targetLatencyInSeconds)) {
                // the player is not aiming for its target latency, so we assume it is in DVR mode
                // (time shifted, or paused/resumed without catching up): the latency would include the time shift
                return
            }

            latencyMeter.addMeasurement(player.lowLatency.latency, targetLatencyInSeconds)
        } catch (e: Exception) {
            BitmovinLog.e(TAG, e.message, e)
        }
    }

    // A request that is not answered (e.g. because the player cancelled it internally) must not block
    // latency tracking forever, so it is considered lost after a while.
    private fun isRequestInFlight(): Boolean {
        val since = requestInFlightSince ?: return false
        return elapsedTimeProvider() - since < REQUEST_TIMEOUT_IN_MS
    }

    companion object {
        private const val TAG = "LiveLatencyTracker"

        // The effective target can deviate slightly from the configured one (e.g. clamping to the
        // manifest's allowed range or small adjustments after rebuffering), so the comparison needs a tolerance.
        const val TARGET_LATENCY_TOLERANCE_IN_SECONDS = 1.0

        const val REQUEST_TIMEOUT_IN_MS = 2000L

        // An effective target this close to the live edge is low latency playback by definition,
        // no matter what target is configured, so it is always tracked.
        const val ALWAYS_TRACKED_TARGET_PLAYBACK_LATENCY_IN_SECONDS = 3.0

        /**
         * Latency is tracked when the effective target playback latency of the player is at most
         * [ALWAYS_TRACKED_TARGET_PLAYBACK_LATENCY_IN_SECONDS], or when it is within
         * [TARGET_LATENCY_TOLERANCE_IN_SECONDS] of the (configured or media defined) target latency. Any larger
         * deviation means the player is not playing at its target latency, which we treat as DVR mode.
         * Unknown values (null, NaN, infinite) never count as playing at the target latency.
         */
        fun isPlayingAtTargetLatency(
            targetPlaybackLatencyInSeconds: Double?,
            targetLatencyInSeconds: Double?,
        ): Boolean {
            if (targetPlaybackLatencyInSeconds == null || !targetPlaybackLatencyInSeconds.isFinite()) {
                return false
            }
            if (targetPlaybackLatencyInSeconds <= ALWAYS_TRACKED_TARGET_PLAYBACK_LATENCY_IN_SECONDS) {
                return true
            }
            if (targetLatencyInSeconds == null) {
                return false
            }

            // in case the user configured target latency is higher
            // than the one from the player internally (could be defined by the manifest
            // we are still tracking the latency, since this is not user induced latency
            if (targetPlaybackLatencyInSeconds < targetLatencyInSeconds) {
                return true
            }

            return abs(targetPlaybackLatencyInSeconds - targetLatencyInSeconds) <= TARGET_LATENCY_TOLERANCE_IN_SECONDS
        }
    }
}
