package com.bitmovin.analytics;

import android.os.Parcel;
import android.os.Parcelable;

public class CollectorConfig implements Parcelable {
    private String backendUrl = "https://analytics-ingress-global.bitmovin.com/";

    private Boolean tryResendDataOnFailedConnection = false;

    public CollectorConfig() {
    }

    protected CollectorConfig(Parcel in) {
        backendUrl = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(backendUrl);
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
     * Get if failed requests should be resent again
     *
     * @return
     */
    public Boolean getTryResendDataOnFailedConnection() {
        return tryResendDataOnFailedConnection;
    }

    /**
     * Set if failed requests should be resent again
     *
     * @param tryResendDataOnFailedConnection
     */
    public void setTryResendDataOnFailedConnection(Boolean tryResendDataOnFailedConnection) {
        this.tryResendDataOnFailedConnection = tryResendDataOnFailedConnection;
    }

}
