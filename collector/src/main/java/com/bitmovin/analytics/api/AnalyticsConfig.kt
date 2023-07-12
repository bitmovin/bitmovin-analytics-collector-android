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
     * Value indicating if ads tracking is disabled.
     */
    val adTrackingDisabled: Boolean = false,

    /**
     * Generate random UserId value for session
     */
    val randomizeUserId: Boolean = false,

    /**
     * The URL of the Bitmovin Analytics backend.
     */
    val backendUrl: String = DEFAULT_BACKEND_URL,

    /**
     * Specifies if failed requests should be resent again. Without permanent storage.
     */
    val inMemoryRetryEnabled: Boolean = false,

    /**
     * Specifies if failed requests should be resent again. With permanent storage.
     * See https://developer.bitmovin.com/playback/docs/is-tracking-of-analytics-data-support-in-offline-mode for more information.
     */
    val longTermRetryEnabled: Boolean = false,
) : Parcelable {
    companion object {
        internal const val DEFAULT_BACKEND_URL = "https://analytics-ingress-global.bitmovin.com/"
    }
}
