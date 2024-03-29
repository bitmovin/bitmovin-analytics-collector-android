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
     *
     * Default is `false`
     */
    val adTrackingDisabled: Boolean = false,

    /**
     * Generate a random UserId for the session
     *
     * Default is `false`
     */
    val randomizeUserId: Boolean = false,

    /**
     * Specifies the retry behavior in case an analytics request cannot be sent to the analytics backend.
     * See [RetryPolicy] for the available settings.
     *
     * Default is [RetryPolicy.NO_RETRY]
     */
    val retryPolicy: RetryPolicy = RetryPolicy.NO_RETRY,

    /**
     * The URL of the Bitmovin Analytics backend.
     *
     * Default is the bitmovin backend URL
     */
    val backendUrl: String = DEFAULT_BACKEND_URL,

) : Parcelable {
    companion object {
        internal const val DEFAULT_BACKEND_URL = "https://analytics-ingress-global.bitmovin.com/"
    }

    class Builder(val licenseKey: String) {
        private var adTrackingDisabled: Boolean = false
        private var randomizeUserId: Boolean = false
        private var retryPolicy: RetryPolicy = RetryPolicy.NO_RETRY
        private var backendUrl: String = DEFAULT_BACKEND_URL

        fun setAdTrackingDisabled(adTrackingDisabled: Boolean) = apply { this.adTrackingDisabled = adTrackingDisabled }
        fun setRandomizeUserId(randomizeUserId: Boolean) = apply { this.randomizeUserId = randomizeUserId }
        fun setRetryPolicy(retryPolicy: RetryPolicy) = apply { this.retryPolicy = retryPolicy }
        fun setBackendUrl(backendUrl: String) = apply { this.backendUrl = backendUrl }

        fun build(): AnalyticsConfig {
            return AnalyticsConfig(
                licenseKey = licenseKey,
                adTrackingDisabled = adTrackingDisabled,
                randomizeUserId = randomizeUserId,
                retryPolicy = retryPolicy,
                backendUrl = backendUrl,
            )
        }
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
