package com.bitmovin.analytics.enums;

import android.os.Parcel;
import android.os.Parcelable;

public enum PlayerType implements Parcelable {
    BITMOVIN("bitmovin"),
    EXOPLAYER("exoplayer"),
    AMAZON_IVS("amazonivs"); // TODO: check uppercase vs lowercase (we use Android:Exoplayer for PLAYER_TECH on the others)

    private final String name;

    PlayerType(String name) {
        this.name = name;
    }

    public static final Creator<PlayerType> CREATOR =
            new Creator<PlayerType>() {
                @Override
                public PlayerType createFromParcel(Parcel in) {
                    return valueOf(in.readString());
                }

                @Override
                public PlayerType[] newArray(int size) {
                    return new PlayerType[size];
                }
            };

    public String toString() {
        return this.name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
    }
}
