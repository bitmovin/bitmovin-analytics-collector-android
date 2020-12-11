package com.bitmovin.analytics.utils;

import android.content.Context;
import android.util.Log;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClient {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private static final String TAG = "HttpClient";
    private OkHttpClient client = null;
    private Context context;
    private BitmovinAnalyticsConfig analyticsConfig;

    public HttpClient(Context context, BitmovinAnalyticsConfig analyticsConfig) {
        this.context = context;
        this.analyticsConfig = analyticsConfig;
    }


    public void post(String url, String postBody, final Callback callback) {
        if (client == null) {
            if (analyticsConfig.isResendDataOnHttpTimeout()){
                client = new OkHttpClient.Builder()

                        .retryOnConnectionFailure(false)
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS).build();
            } else {
                client = new OkHttpClient();
            }
        }

        Log.d(TAG, String.format("Posting Analytics JSON: \n%s\n", postBody));
        Request request = new Request.Builder()
                .url(url)
                .header("Origin", String.format("http://%s", context.getPackageName()))
                .post(RequestBody.create(JSON, postBody))
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "HTTP Error: ", e);
                if (callback != null) {
                    callback.onFailure(call, e);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (callback != null) {
                    callback.onResponse(call, response);
                }
                if (response == null) {
                    return;
                }
                response.close();
            }
        });
    }
}
