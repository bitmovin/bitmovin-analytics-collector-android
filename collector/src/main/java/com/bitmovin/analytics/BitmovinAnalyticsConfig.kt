package com.bitmovin.analytics

import android.os.Parcel
import android.os.Parcelable
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.utils.Util.HEARTBEAT_INTERVAL

/**
 * Deprecated: Use AnalyticsConfig, DefaultMetadata and SourceMetadata instead
 *
 * For a detailed migration guide from API v2 to v3 see:
 * https://developer.bitmovin.com/playback/docs/android-collector-migration-guide-from-api-v2-to-v3
 */
@Deprecated(
    "Use AnalyticsConfig, DefaultMetadata and SourceMetadata instead",
    ReplaceWith("AnalyticsConfig(licenseKey)", "com.bitmovin.analytics.api.AnalyticsConfig"),
)
class BitmovinAnalyticsConfig() : Parcelable {
    constructor(key: String, playerKey: String) : this() {
        this.key = key
        this.playerKey = playerKey
    }

    constructor(key: String) : this(key, "")

    var key: String = ""
        private set

    var playerKey: String = ""
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
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData31: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData32: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData33: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData34: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData35: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData36: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData37: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData38: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData39: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData40: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData41: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData42: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData43: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData44: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData45: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData46: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData47: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData48: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData49: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData50: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData51: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData52: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData53: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData54: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData55: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData56: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData57: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData58: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData59: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData60: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData61: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData62: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData63: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData64: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData65: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData66: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData67: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData68: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData69: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData70: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData71: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData72: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData73: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData74: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData75: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData76: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData77: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData78: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData79: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData80: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData81: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData82: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData83: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData84: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData85: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData86: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData87: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData88: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData89: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData90: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData91: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData92: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData93: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData94: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData95: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData96: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData97: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData98: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData99: String? = null

    /**
     * Optional free-form data Not enabled by default Must be activated for your organization
     */
    var customData100: String? = null

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
    @Deprecated("PlayerType is determined automatically and cannot be configured")
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
        customData31 = parcel.readString()
        customData32 = parcel.readString()
        customData33 = parcel.readString()
        customData34 = parcel.readString()
        customData35 = parcel.readString()
        customData36 = parcel.readString()
        customData37 = parcel.readString()
        customData38 = parcel.readString()
        customData39 = parcel.readString()
        customData40 = parcel.readString()
        customData41 = parcel.readString()
        customData42 = parcel.readString()
        customData43 = parcel.readString()
        customData44 = parcel.readString()
        customData45 = parcel.readString()
        customData46 = parcel.readString()
        customData47 = parcel.readString()
        customData48 = parcel.readString()
        customData49 = parcel.readString()
        customData50 = parcel.readString()
        customData51 = parcel.readString()
        customData52 = parcel.readString()
        customData53 = parcel.readString()
        customData54 = parcel.readString()
        customData55 = parcel.readString()
        customData56 = parcel.readString()
        customData57 = parcel.readString()
        customData58 = parcel.readString()
        customData59 = parcel.readString()
        customData60 = parcel.readString()
        customData61 = parcel.readString()
        customData62 = parcel.readString()
        customData63 = parcel.readString()
        customData64 = parcel.readString()
        customData65 = parcel.readString()
        customData66 = parcel.readString()
        customData67 = parcel.readString()
        customData68 = parcel.readString()
        customData69 = parcel.readString()
        customData70 = parcel.readString()
        customData71 = parcel.readString()
        customData72 = parcel.readString()
        customData73 = parcel.readString()
        customData74 = parcel.readString()
        customData75 = parcel.readString()
        customData76 = parcel.readString()
        customData77 = parcel.readString()
        customData78 = parcel.readString()
        customData79 = parcel.readString()
        customData80 = parcel.readString()
        customData81 = parcel.readString()
        customData82 = parcel.readString()
        customData83 = parcel.readString()
        customData84 = parcel.readString()
        customData85 = parcel.readString()
        customData86 = parcel.readString()
        customData87 = parcel.readString()
        customData88 = parcel.readString()
        customData89 = parcel.readString()
        customData90 = parcel.readString()
        customData91 = parcel.readString()
        customData92 = parcel.readString()
        customData93 = parcel.readString()
        customData94 = parcel.readString()
        customData95 = parcel.readString()
        customData96 = parcel.readString()
        customData97 = parcel.readString()
        customData98 = parcel.readString()
        customData99 = parcel.readString()
        customData100 = parcel.readString()
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

    override fun writeToParcel(
        dest: Parcel,
        flags: Int,
    ) {
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
        dest.writeString(customData31)
        dest.writeString(customData32)
        dest.writeString(customData33)
        dest.writeString(customData34)
        dest.writeString(customData35)
        dest.writeString(customData36)
        dest.writeString(customData37)
        dest.writeString(customData38)
        dest.writeString(customData39)
        dest.writeString(customData40)
        dest.writeString(customData41)
        dest.writeString(customData42)
        dest.writeString(customData43)
        dest.writeString(customData44)
        dest.writeString(customData45)
        dest.writeString(customData46)
        dest.writeString(customData47)
        dest.writeString(customData48)
        dest.writeString(customData49)
        dest.writeString(customData50)
        dest.writeString(customData51)
        dest.writeString(customData52)
        dest.writeString(customData53)
        dest.writeString(customData54)
        dest.writeString(customData55)
        dest.writeString(customData56)
        dest.writeString(customData57)
        dest.writeString(customData58)
        dest.writeString(customData59)
        dest.writeString(customData60)
        dest.writeString(customData61)
        dest.writeString(customData62)
        dest.writeString(customData63)
        dest.writeString(customData64)
        dest.writeString(customData65)
        dest.writeString(customData66)
        dest.writeString(customData67)
        dest.writeString(customData68)
        dest.writeString(customData69)
        dest.writeString(customData70)
        dest.writeString(customData71)
        dest.writeString(customData72)
        dest.writeString(customData73)
        dest.writeString(customData74)
        dest.writeString(customData75)
        dest.writeString(customData76)
        dest.writeString(customData77)
        dest.writeString(customData78)
        dest.writeString(customData79)
        dest.writeString(customData80)
        dest.writeString(customData81)
        dest.writeString(customData82)
        dest.writeString(customData83)
        dest.writeString(customData84)
        dest.writeString(customData85)
        dest.writeString(customData86)
        dest.writeString(customData87)
        dest.writeString(customData88)
        dest.writeString(customData89)
        dest.writeString(customData90)
        dest.writeString(customData91)
        dest.writeString(customData92)
        dest.writeString(customData93)
        dest.writeString(customData94)
        dest.writeString(customData95)
        dest.writeString(customData96)
        dest.writeString(customData97)
        dest.writeString(customData98)
        dest.writeString(customData99)
        dest.writeString(customData100)
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
        val CREATOR =
            object : Parcelable.Creator<BitmovinAnalyticsConfig> {
                override fun createFromParcel(parcel: Parcel) = BitmovinAnalyticsConfig(parcel)

                override fun newArray(size: Int) = arrayOfNulls<BitmovinAnalyticsConfig>(size)
            }
    }
}
