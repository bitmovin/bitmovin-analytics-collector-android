package com.bitmovin.analytics.enums

import android.os.Parcel
import android.os.Parcelable

enum class PlayerType(private val value: String) : Parcelable {
    BITMOVIN("bitmovin"),
    EXOPLAYER("exoplayer"),
    AMAZON_IVS("amazonivs"),
    MEDIA3_EXOPLAYER("media3-exoplayer"),
    THEOPLAYER("theoplayer"),
    ;

    override fun toString(): String {
        return value
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(
        parcel: Parcel,
        i: Int,
    ) {
        parcel.writeString(value)
    }

    companion object {
        @JvmField
        val CREATOR =
            object : Parcelable.Creator<PlayerType> {
                override fun createFromParcel(parcel: Parcel): PlayerType {
                    // default to bitmovin to be on the save side, otherwise we have to use '!!' to convert String? to String
                    return valueOf(parcel.readString() ?: BITMOVIN.toString())
                }

                override fun newArray(size: Int) = arrayOfNulls<PlayerType>(size)
            }
    }
}
