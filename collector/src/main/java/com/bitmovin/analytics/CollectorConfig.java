package com.bitmovin.analytics;

import android.os.Parcel;
import android.os.Parcelable;

public class CollectorConfig implements Parcelable {
    private String backendUrl = "https://analytics-ingress-global.bitmovin.com/";
    private Boolean isLive;

    public CollectorConfig() {
    }

    protected CollectorConfig(Parcel in) {
        backendUrl = in.readString();
        isLive = (Boolean) in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(backendUrl);
        dest.writeSerializable(isLive);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<CollectorConfig> CREATOR = new Creator<CollectorConfig>() {
        @Override
        public CollectorConfig createFromParcel(Parcel in) {
            return new CollectorConfig(in);
        }

        @Override
        public CollectorConfig[] newArray(int size) {
            return new CollectorConfig[size];
        }
    };

    /**
     * Get the URL of the Bitmovin Analytics backend.
     *
     * @return
     */
    public String getBackendUrl() {
        return backendUrl;
    }

    /**
     * Set the URL of the Bitmovin Analytics backend to interact with.
     * Used for on-premise deployments of Bitmovin Analytics
     *
     * @param backendUrl
     */
    public void setBackendUrl(String backendUrl) {
        this.backendUrl = backendUrl;
    }

    /**
     * Returns true if the stream is live.
     * @return
     */
    public Boolean isLive() {
        return isLive;
    }

    public void setIsLive(Boolean live) {
        isLive = live;
    }
}
