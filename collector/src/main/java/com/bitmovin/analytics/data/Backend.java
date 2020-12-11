package com.bitmovin.analytics.data;

import okhttp3.Callback;

public interface Backend {
    void send(EventData eventData, Callback callback);
    void sendAd(AdEventData eventData, Callback callback);
}
