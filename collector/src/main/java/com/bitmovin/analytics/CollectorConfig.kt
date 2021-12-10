package com.bitmovin.analytics

import android.os.Parcel
import android.os.Parcelable

class CollectorConfig() : Parcelable {
    /**
     * The URL of the Bitmovin Analytics backend.
     */
    var backendUrl = DEFAULT_BACKEND_URL

    /**
     * Specifies if failed requests should be resent again.
     */
    var tryResendDataOnFailedConnection = false

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
        private const val DEFAULT_BACKEND_URL = "https://analytics-ingress-global.bitmovin.com/"

        @JvmField
        val CREATOR = object : Parcelable.Creator<CollectorConfig> {
            override fun createFromParcel(parcel: Parcel) = CollectorConfig(parcel)
            override fun newArray(size: Int) = arrayOfNulls<CollectorConfig>(size)
        }
    }
}
