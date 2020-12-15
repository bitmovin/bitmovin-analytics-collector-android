package com.bitmovin.analytics.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bitmovin.analytics.CollectorConfig;
import com.bitmovin.analytics.utils.DataSerializer;
import com.bitmovin.analytics.utils.HttpClient;

import org.jetbrains.annotations.NotNull;

import okhttp3.Callback;

public class HttpBackend implements Backend, CallbackBackend {
    private static final String TAG = "BitmovinBackend";
    private HttpClient httpClient;
    private String analyticsBackendUrl;
    private String adsAnalyticsBackendUrl;

    public HttpBackend(CollectorConfig config, Context context) {
        analyticsBackendUrl = Uri.parse(config.getBackendUrl()).buildUpon().appendEncodedPath("analytics").build().toString();
        adsAnalyticsBackendUrl = Uri.parse(config.getBackendUrl()).buildUpon().appendEncodedPath("analytics/a").build().toString();
        Log.d(TAG, String.format("Initialized Analytics HTTP Backend with %s", analyticsBackendUrl));
        this.httpClient = new HttpClient(context, config.getTryResendDataOnFailedConnection());
    }

    @Override
    public void send(EventData eventData, Callback callback) {
        Log.d(TAG, String.format("Sending sample: %s (state: %s, videoId: %s, startupTime: %d, videoStartupTime: %d, buffered: %d, audioLanguage: %s)",
                eventData.getImpressionId(),
                eventData.getVideoId(),
                eventData.getState(),
                eventData.getStartupTime(),
                eventData.getVideoStartupTime(),
                eventData.getBuffered(),
                eventData.getAudioLanguage()));
        this.httpClient.post(analyticsBackendUrl, DataSerializer.serialize(eventData), callback);
    }

    @Override
    public void sendAd(AdEventData eventData, Callback callback) {
        Log.d(TAG, String.format("Sending ad sample: %s (videoImpressionId: %s, adImpressionId: %s)",
                eventData.getAdImpressionId(),
                eventData.getVideoImpressionId(),
                eventData.getAdImpressionId()));
        this.httpClient.post(adsAnalyticsBackendUrl, DataSerializer.serialize(eventData), callback);
    }

    @Override
    public void send(@NotNull EventData eventData) {
        this.send(eventData, null);
    }

    @Override
    public void sendAd(@NotNull AdEventData eventData) {
        this.sendAd(eventData, null);
    }
}
