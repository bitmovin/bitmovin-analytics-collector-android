package com.bitmovin.analytics.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.utils.DataSerializer;
import com.bitmovin.analytics.utils.HttpClient;

public class HttpBackend implements Backend {
    private static final String TAG = "BitmovinAnalytics/Backend";
    private HttpClient httpClient;

    public HttpBackend(BitmovinAnalyticsConfig config, Context context) {
        String backendUrl = Uri.parse(config.getCollectorConfig().getBackendUrl()).buildUpon().appendEncodedPath("analytics").build().toString();
        Log.d(TAG, String.format("Initialized Analytics HTTP Backend with %s", backendUrl));
        this.httpClient = new HttpClient(context, backendUrl);
    }

    @Override
    public void send(EventData eventData) {
        Log.d(TAG, String.format("Sending sample: %s (startupTime: %d, videoStartupTime: %d)", eventData.getImpressionId(), eventData.getStartupTime(), eventData.getVideoStartupTime()));
        this.httpClient.post(DataSerializer.serialize(eventData), null);
    }
}
