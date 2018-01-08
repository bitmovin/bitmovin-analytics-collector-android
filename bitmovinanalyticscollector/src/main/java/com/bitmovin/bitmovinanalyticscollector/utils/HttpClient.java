package com.bitmovin.bitmovinanalyticscollector.utils;

import android.util.Log;

import com.bitmovin.bitmovinanalyticscollector.analytics.BitmovinAnalyticsConfig;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
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

        Log.d(TAG, String.format("Posting Analytics JSON: \n%s\n", postBody));
        Request request = new Request.Builder()
                .url(bitmovinAnalyticsConfig.analyticsUrl)
                .header("Origin", String.format("http://%s", bitmovinAnalyticsConfig.getContext().getPackageName()))
                .post(RequestBody.create(JSON, postBody))
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "HTTP Error: ", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null) {
                    Log.i(TAG, String.format("Analytics HTTP response: %d", response.code()));
                    response.close();
                }
            }
        });
    }
}
