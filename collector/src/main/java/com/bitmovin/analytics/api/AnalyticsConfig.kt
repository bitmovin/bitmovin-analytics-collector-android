package com.bitmovin.analytics.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AnalyticsConfig
@JvmOverloads
constructor(
    /**
     * The analytics license key
     */
    val licenseKey: String,

    /**
     * Value indicating if ad tracking is disabled.
     */
    val adTrackingDisabled: Boolean = false,

    /**
     * Generate a random UserId for the session
     */
    val randomizeUserId: Boolean = false,

    /**
     * The URL of the Bitmovin Analytics backend.
     */
    val backendUrl: String = DEFAULT_BACKEND_URL,

    /**
     * Specifies the retry behavior in case an analytics request cannot be sent to the analytics backend.
     * See [RetryPolicy] for the available settings.
     */
    val retryPolicy: RetryPolicy = RetryPolicy.NO_RETRY,

) : Parcelable {
    companion object {
        internal const val DEFAULT_BACKEND_URL = "https://analytics-ingress-global.bitmovin.com/"
    }
}

enum class RetryPolicy {

    /**
     * No retry in case an analytics request cannot be sent to the analytics backend
     */
    NO_RETRY,

    /**
     * A failing request is retried for a maximum of 300 seconds, while the collector instance
     * is still alive. The initial license call to verify the analytics license needs to be successful.
     */
    SHORT_TERM,

    /**
     * A failing request is retried for up to 14 days. The analytics request is stored
     * permanently until it is sent successfully or the max lifetime is reached.
     * This policy can be used for tracking of offline playback.
     *
     * See https://developer.bitmovin.com/playback/docs/is-tracking-of-analytics-data-support-in-offline-mode
     * for more information.
     */
    LONG_TERM,
}
