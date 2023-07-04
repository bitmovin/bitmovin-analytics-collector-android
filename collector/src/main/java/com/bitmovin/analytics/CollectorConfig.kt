package com.bitmovin.analytics

import android.os.Parcel
import android.os.Parcelable

@Deprecated("Use AnalyticsConfig instead")
class CollectorConfig() : Parcelable {
    /**
     * The URL of the Bitmovin Analytics backend.
     */
    var backendUrl = DEFAULT_BACKEND_URL

    /**
     * Specifies if failed requests should be resent again.
     */
    var tryResendDataOnFailedConnection = false

    /**
     * When set to `true`, analytics events that have failed to be sent are cached in a persistent way. Cached elements
     * from within 14 days are retried at a later point in time, once a network connection is established again. There
     * will be at most 5,000 elements cached in total and at most 500 elements per playback session.
     *
     * Disabling this config flag does not disable the retrying of already cached elements.
     */
    var longTermRetryEnabled = false

    private constructor(parcel: Parcel) : this() {
        backendUrl = parcel.readString() ?: DEFAULT_BACKEND_URL
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(backendUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        internal const val DEFAULT_BACKEND_URL = "https://analytics-ingress-global.bitmovin.com/"

        @JvmField
        val CREATOR = object : Parcelable.Creator<CollectorConfig> {
            override fun createFromParcel(parcel: Parcel) = CollectorConfig(parcel)
            override fun newArray(size: Int) = arrayOfNulls<CollectorConfig>(size)
        }
    }
}
