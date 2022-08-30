package com.bitmovin.analytics

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.utils.Util.HEARTBEAT_INTERVAL

class BitmovinAnalyticsConfig() : Parcelable {
    constructor(key: String, playerKey: String) : this() {
        this.key = key
        this.playerKey = playerKey
    }

    constructor(key: String) : this(key, "")

    @Deprecated("")
    constructor(key: String, context: Context?) : this(key) {
        this.context = context
    }

    @Deprecated("")
    constructor(key: String, playerKey: String, context: Context?) : this(key, playerKey) {
        this.context = context
    }

    var key: String = ""
        private set

    var playerKey: String = ""
        private set

    var context: Context? = null
        private set

    /**
     * CDN Provider used to play out Content
     */
    var cdnProvider: String? = null

    /**
     * Optional free-form data
     */
    var customData1: String? = null

    /**
     * Optional free-form data
     */
    var customData2: String? = null

    /**
     * Optional free-form data
     */
    var customData3: String? = null

    /**
     * Optional free-form data
     */
    var customData4: String? = null

    /**
     * Optional free-form data
     */
    var customData5: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData6: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData7: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData8: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData9: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData10: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData11: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData12: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData13: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData14: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData15: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData16: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData17: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData18: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData19: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData20: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData21: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData22: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData23: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData24: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData25: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData26: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData27: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData28: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData29: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData30: String? = null

    /**
     * User-ID in the Customer System
     */
    var customUserId: String? = null

    /**
     * A/B Test Experiment Name
     */
    var experimentName: String? = null

    /**
     * Set MPD URL recorded in analytics. If not set explicitly the collector will retrieve
     * available information from the player.
     */
    var mpdUrl: String? = null

    /**
     * Set M3U8 URL recorded in analytics. If not set explicitly the collector will retrieve
     * available information from the player.
     */
    var m3u8Url: String? = null

    /**
     * Set progUrl URL recorded in analytics. If not set explicitly the collector will retrieve
     * available information from the player.
     */
    var progUrl: String? = null

    /**
     * The frequency that heartbeats should be sent, in milliseconds
     */
    @Deprecated("No longer possible to change default value of 59700ms")
    var heartbeatInterval = HEARTBEAT_INTERVAL

    /**
     * Human readable title of the video asset currently playing
     */
    var title: String? = null

    /**
     * Breadcrumb path
     */
    var path: String? = null

    /**
     * PlayerType that the current video is being played back with.
     */
    var playerType: PlayerType? = null

    /**
     * ID of the Video in the Customer System
     */
    var videoId: String? = null

    /**
     * Value indicating if ads tracking is enabled
     */
    var ads = true

    /**
     * Mark the stream as live before stream metadata is available.
     */
    @set:JvmName("setIsLive")
    @get:JvmName("getIsLive")
    var isLive: Boolean? = null

    /**
     * Generate random UserId value for session
     */
    var randomizeUserId = false

    /**
     * Configuration options for the Analytics collector
     */
    var config = CollectorConfig()
        private set

    private constructor(parcel: Parcel) : this() {
        cdnProvider = parcel.readString()
        customData1 = parcel.readString()
        customData2 = parcel.readString()
        customData3 = parcel.readString()
        customData4 = parcel.readString()
        customData5 = parcel.readString()
        customData6 = parcel.readString()
        customData7 = parcel.readString()
        customData8 = parcel.readString()
        customData9 = parcel.readString()
        customData10 = parcel.readString()
        customData11 = parcel.readString()
        customData12 = parcel.readString()
        customData13 = parcel.readString()
        customData14 = parcel.readString()
        customData15 = parcel.readString()
        customData16 = parcel.readString()
        customData17 = parcel.readString()
        customData18 = parcel.readString()
        customData19 = parcel.readString()
        customData20 = parcel.readString()
        customData21 = parcel.readString()
        customData22 = parcel.readString()
        customData23 = parcel.readString()
        customData24 = parcel.readString()
        customData25 = parcel.readString()
        customData26 = parcel.readString()
        customData27 = parcel.readString()
        customData28 = parcel.readString()
        customData29 = parcel.readString()
        customData30 = parcel.readString()
        customUserId = parcel.readString()
        experimentName = parcel.readString()
        mpdUrl = parcel.readString()
        m3u8Url = parcel.readString()
        heartbeatInterval = parcel.readInt()
        key = parcel.readString() ?: ""
        title = parcel.readString()
        path = parcel.readString()
        playerKey = parcel.readString() ?: ""
        playerType = parcel.readParcelable(PlayerType::class.java.classLoader)
        videoId = parcel.readString()
        isLive = parcel.readSerializable() as Boolean
        config = parcel.readParcelable(CollectorConfig::class.java.classLoader) ?: CollectorConfig()
        ads = parcel.readInt() == 1
        randomizeUserId = parcel.readSerializable() as Boolean
        progUrl = parcel.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(cdnProvider)
        dest.writeString(customData1)
        dest.writeString(customData2)
        dest.writeString(customData3)
        dest.writeString(customData4)
        dest.writeString(customData5)
        dest.writeString(customData6)
        dest.writeString(customData7)
        dest.writeString(customData8)
        dest.writeString(customData9)
        dest.writeString(customData10)
        dest.writeString(customData11)
        dest.writeString(customData12)
        dest.writeString(customData13)
        dest.writeString(customData14)
        dest.writeString(customData15)
        dest.writeString(customData16)
        dest.writeString(customData17)
        dest.writeString(customData18)
        dest.writeString(customData19)
        dest.writeString(customData20)
        dest.writeString(customData21)
        dest.writeString(customData22)
        dest.writeString(customData23)
        dest.writeString(customData24)
        dest.writeString(customData25)
        dest.writeString(customData26)
        dest.writeString(customData27)
        dest.writeString(customData28)
        dest.writeString(customData29)
        dest.writeString(customData30)
        dest.writeString(customUserId)
        dest.writeString(experimentName)
        dest.writeString(mpdUrl)
        dest.writeString(m3u8Url)
        dest.writeInt(heartbeatInterval)
        dest.writeString(key)
        dest.writeString(title)
        dest.writeString(path)
        dest.writeString(playerKey)
        dest.writeParcelable(playerType, flags)
        dest.writeString(videoId)
        dest.writeSerializable(isLive)
        dest.writeParcelable(config, config.describeContents())
        dest.writeInt(if (ads) 1 else 0)
        dest.writeSerializable(randomizeUserId)
        dest.writeString(progUrl)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<BitmovinAnalyticsConfig> {
            override fun createFromParcel(parcel: Parcel) = BitmovinAnalyticsConfig(parcel)
            override fun newArray(size: Int) = arrayOfNulls<BitmovinAnalyticsConfig>(size)
        }
    }
}
