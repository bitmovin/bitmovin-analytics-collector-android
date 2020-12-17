package com.bitmovin.analytics.data;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bitmovin.analytics.CollectorConfig;
import com.bitmovin.analytics.retryBackend.OnFailureCallback;
import com.bitmovin.analytics.utils.ClientFactory;
import com.bitmovin.analytics.utils.DataSerializer;
import com.bitmovin.analytics.utils.HttpClient;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class HttpBackend implements Backend, CallbackBackend {
    private static final String TAG = "BitmovinBackend";
    private HttpClient httpClient;
    private String analyticsBackendUrl;
    private String adsAnalyticsBackendUrl;

    public HttpBackend(CollectorConfig config, Context context) {
        analyticsBackendUrl = Uri.parse(config.getBackendUrl()).buildUpon().appendEncodedPath("analytics").build().toString();
        adsAnalyticsBackendUrl = Uri.parse(config.getBackendUrl()).buildUpon().appendEncodedPath("analytics/a").build().toString();
        Log.d(TAG, String.format("Initialized Analytics HTTP Backend with %s", analyticsBackendUrl));
        this.httpClient = new HttpClient(context, new ClientFactory().createClient(config));
    }

    @Override
    public void send(EventData eventData) {
        this.send(eventData, null);
    }

    @Override
    public void sendAd(AdEventData eventData) {
        this.sendAd(eventData, null);
    }

    @Override
    public void send(EventData eventData, OnFailureCallback callback) {
        Log.d(TAG, String.format("Sending sample: %s (state: %s, videoId: %s, startupTime: %d, videoStartupTime: %d, buffered: %d, audioLanguage: %s)",
                eventData.getImpressionId(),
                eventData.getVideoId(),
                eventData.getState(),
                eventData.getStartupTime(),
                eventData.getVideoStartupTime(),
                eventData.getBuffered(),
                eventData.getAudioLanguage()));
        this.httpClient.post(analyticsBackendUrl, DataSerializer.serialize(eventData), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(callback != null) {
                    callback.onFailure(e, () -> { call.cancel(); return  null;});
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
            }
        });
    }

    @Override
    public void sendAd(AdEventData eventData, OnFailureCallback callback) {
        Log.d(TAG, String.format("Sending ad sample: %s (videoImpressionId: %s, adImpressionId: %s)",
                eventData.getAdImpressionId(),
                eventData.getVideoImpressionId(),
                eventData.getAdImpressionId()));
        this.httpClient.post(adsAnalyticsBackendUrl, DataSerializer.serialize(eventData), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if(callback !=null) {
                    callback.onFailure(e, () ->{ call.cancel(); return  null;});
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
            }
        });
    }

}
