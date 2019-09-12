package com.bitmovin.analytics.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bitmovin.analytics.CollectorConfig;
import com.bitmovin.analytics.utils.DataSerializer;
import com.bitmovin.analytics.utils.HttpClient;

public class HttpBackend implements Backend {
    private static final String TAG = "BitmovinBackend";
    private HttpClient httpClient;
    private String analyticsBackendUrl;
    private String adsAnalyticsBackendUrl;

    public HttpBackend(CollectorConfig config, Context context) {
        analyticsBackendUrl = Uri.parse(config.getBackendUrl()).buildUpon().appendEncodedPath("analytics").build().toString();
        adsAnalyticsBackendUrl = Uri.parse(config.getBackendUrl()).buildUpon().appendEncodedPath("analytics/a").build().toString();
        Log.d(TAG, String.format("Initialized Analytics HTTP Backend with %s", analyticsBackendUrl));
        this.httpClient = new HttpClient(context);
    }

    @Override
    public void send(EventData eventData) {
        Log.d(TAG, String.format("Sending sample: %s (state: %s, videoId: %s, startupTime: %d, videoStartupTime: %d, buffered: %d)",
                eventData.getImpressionId(),
                eventData.getVideoId(),
                eventData.getState(),
                eventData.getStartupTime(),
                eventData.getVideoStartupTime(),
                eventData.getBuffered()));
        this.httpClient.post(analyticsBackendUrl, DataSerializer.serialize(eventData), null);
    }

    @Override
    public void sendAd(AdEventData eventData) {
        Log.d(TAG, String.format("Sending ad sample: %s (videoImpressionId: %s, adImpressionId: %s)",
                eventData.getVideoImpressionId(),
                eventData.getAdImpressionId()));
        this.httpClient.post(adsAnalyticsBackendUrl, DataSerializer.serialize(eventData), null);
    }
}
