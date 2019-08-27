package com.bitmovin.analytics.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bitmovin.analytics.CollectorConfig;
import com.bitmovin.analytics.utils.DataSerializer;
import com.bitmovin.analytics.utils.HttpClient;

public class HttpBackend implements Backend {
    private static final String TAG = "Backend";
    private HttpClient httpClient;

    public HttpBackend(CollectorConfig config, Context context) {
        String backendUrl = Uri.parse(config.getBackendUrl()).buildUpon().appendEncodedPath("analytics").build().toString();
        Log.d(TAG, String.format("Initialized Analytics HTTP Backend with %s", backendUrl));
        this.httpClient = new HttpClient(context, backendUrl);
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
        this.httpClient.post(DataSerializer.serialize(eventData), null);
    }
}
