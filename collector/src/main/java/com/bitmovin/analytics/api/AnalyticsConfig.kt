package com.bitmovin.analytics.api

import android.os.Parcel
import android.os.Parcelable
import com.bitmovin.analytics.CollectorConfig

data class AnalyticsConfig
@JvmOverloads
constructor(
    val key: String = "",
    val playerKey: String = "",
    /**
     * CDN Provider used to play out Content
     */
    val cdnProvider: String? = null,

    /**
     * Optional free-form data
     */
    val customData1: String? = null,

    /**
     * Optional free-form data
     */
    val customData2: String? = null,

    /**
     * Optional free-form data
     */
    val customData3: String? = null,

    /**
     * Optional free-form data
     */
    val customData4: String? = null,

    /**
     * Optional free-form data
     */
    val customData5: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData6: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData7: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData8: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData9: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData10: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData11: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData12: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData13: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData14: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData15: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData16: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData17: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData18: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData19: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData20: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData21: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData22: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData23: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData24: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData25: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData26: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData27: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData28: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData29: String? = null,

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    val customData30: String? = null,

    /**
     * User-ID in the Customer System
     */
    val customUserId: String? = null,

    /**
     * A/B Test Experiment Name
     */
    val experimentName: String? = null,

    /**
     * Value indicating if ads tracking is enabled
     */
    val ads: Boolean = true,

    /**
     * Generate random UserId value for session
     */
    val randomizeUserId: Boolean = false,

    /**
     * The URL of the Bitmovin Analytics backend.
     */
    val backendUrl: String = CollectorConfig.DEFAULT_BACKEND_URL,

    /**
     * Specifies if failed requests should be resent again.
     */
    val tryResendDataOnFailedConnection: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        // TODO: should we throw, if there is no key in the bundle?
        key = parcel.readString() ?: "",
        // TODO: should we throw, if there is no playerKey in the bundle?
        playerKey = parcel.readString() ?: "",
        cdnProvider = parcel.readString(),
        customData1 = parcel.readString(),
        customData2 = parcel.readString(),
        customData3 = parcel.readString(),
        customData4 = parcel.readString(),
        customData5 = parcel.readString(),
        customData6 = parcel.readString(),
        customData7 = parcel.readString(),
        customData8 = parcel.readString(),
        customData9 = parcel.readString(),
        customData10 = parcel.readString(),
        customData11 = parcel.readString(),
        customData12 = parcel.readString(),
        customData13 = parcel.readString(),
        customData14 = parcel.readString(),
        customData15 = parcel.readString(),
        customData16 = parcel.readString(),
        customData17 = parcel.readString(),
        customData18 = parcel.readString(),
        customData19 = parcel.readString(),
        customData20 = parcel.readString(),
        customData21 = parcel.readString(),
        customData22 = parcel.readString(),
        customData23 = parcel.readString(),
        customData24 = parcel.readString(),
        customData25 = parcel.readString(),
        customData26 = parcel.readString(),
        customData27 = parcel.readString(),
        customData28 = parcel.readString(),
        customData29 = parcel.readString(),
        customData30 = parcel.readString(),
        customUserId = parcel.readString(),
        experimentName = parcel.readString(),
        ads = parcel.readByte() != 0.toByte(),
        randomizeUserId = parcel.readByte() != 0.toByte(),
        // TODO: should we throw, if there is no backendUrl in the bundle?
        backendUrl = parcel.readString() ?: "",
        tryResendDataOnFailedConnection = parcel.readByte() != 0.toByte()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(key)
        parcel.writeString(playerKey)
        parcel.writeString(cdnProvider)
        parcel.writeString(customData1)
        parcel.writeString(customData2)
        parcel.writeString(customData3)
        parcel.writeString(customData4)
        parcel.writeString(customData5)
        parcel.writeString(customData6)
        parcel.writeString(customData7)
        parcel.writeString(customData8)
        parcel.writeString(customData9)
        parcel.writeString(customData10)
        parcel.writeString(customData11)
        parcel.writeString(customData12)
        parcel.writeString(customData13)
        parcel.writeString(customData14)
        parcel.writeString(customData15)
        parcel.writeString(customData16)
        parcel.writeString(customData17)
        parcel.writeString(customData18)
        parcel.writeString(customData19)
        parcel.writeString(customData20)
        parcel.writeString(customData21)
        parcel.writeString(customData22)
        parcel.writeString(customData23)
        parcel.writeString(customData24)
        parcel.writeString(customData25)
        parcel.writeString(customData26)
        parcel.writeString(customData27)
        parcel.writeString(customData28)
        parcel.writeString(customData29)
        parcel.writeString(customData30)
        parcel.writeString(customUserId)
        parcel.writeString(experimentName)
        parcel.writeByte(if (ads) 1 else 0)
        parcel.writeByte(if (randomizeUserId) 1 else 0)
        parcel.writeString(backendUrl)
        parcel.writeByte(if (tryResendDataOnFailedConnection) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AnalyticsConfig> {
        override fun createFromParcel(parcel: Parcel): AnalyticsConfig {
            return AnalyticsConfig(parcel)
        }

        override fun newArray(size: Int): Array<AnalyticsConfig?> {
            return arrayOfNulls(size)
        }
    }
}
