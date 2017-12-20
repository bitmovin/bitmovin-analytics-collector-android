package com.bitmovin.bitmovinanalyticscollector.utils;

import android.util.Log;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by zachmanc on 12/14/17.
 */

public class HttpClient {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private static final String TAG = "HttpClient";
    private final OkHttpClient client = new OkHttpClient();
    private BitmovinAnalyticsConfig bitmovinAnalyticsConfig;

    public HttpClient(BitmovinAnalyticsConfig bitmovinAnalyticsConfig) {
        this.bitmovinAnalyticsConfig = bitmovinAnalyticsConfig;
    }

    public void post(String postBody) {
        Request request = new Request.Builder()
                .url(bitmovinAnalyticsConfig.analyticsUrl)
                .post(RequestBody.create(JSON, postBody))
                .build();

        Response response = null;

        try {
            response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            Log.i(TAG, String.format("%d", response.code()));

        } catch (IOException e) {
            Log.e(TAG, "HTTP Error", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }
}
