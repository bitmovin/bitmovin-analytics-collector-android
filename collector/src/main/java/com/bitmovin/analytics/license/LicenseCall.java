package com.bitmovin.analytics.license;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.BuildConfig;
import com.bitmovin.analytics.CollectorConfig;
import com.bitmovin.analytics.data.LicenseCallData;
import com.bitmovin.analytics.data.LicenseResponse;
import com.bitmovin.analytics.utils.ClientFactory;
import com.bitmovin.analytics.utils.DataSerializer;
import com.bitmovin.analytics.utils.HttpClient;
import com.bitmovin.analytics.utils.Util;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LicenseCall {
    private static final String TAG = "LicenseCall";
    private final String backendUrl;
    private CollectorConfig config;
    private Context context;
    private HttpClient httpClient;

    public LicenseCall(CollectorConfig config, Context context) {
        this.config = config;
        this.context = context;
        this.backendUrl = Uri.parse(config.getBackendUrl()).buildUpon().appendEncodedPath("licensing").build().toString();
        Log.d(TAG, String.format("Initialized License Call with backendUrl: %s", backendUrl));
        this.httpClient = new HttpClient(context, new ClientFactory().createClient(config));
    }

    public void authenticate(final LicenseCallback callback) {
        final LicenseCallData data = new LicenseCallData();
        //data.setKey(this.config.getKey());
         data.setKey("e73a3577-d91c-4214-9e6d-938fb936818a");
        data.setAnalyticsVersion(BuildConfig.VERSION_NAME);
        //data.setDomain(context.getPackageName());
        data.setDomain("localhost");
        String json = DataSerializer.serialize(data);
        httpClient.post(this.backendUrl, json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "License call failed due to connectivity issues", e);
                callback.authenticationCompleted(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response == null || response.body() == null) {
                    Log.d(TAG, "License call was denied without providing a response body");
                    callback.authenticationCompleted(false);
                    return;
                }

                String licensingResponseBody = response.body().string();
                LicenseResponse licenseResponse = DataSerializer.deserialize(licensingResponseBody, LicenseResponse.class);
                if (licenseResponse == null) {
                    Log.d(TAG, "License call was denied without providing a response body");
                    callback.authenticationCompleted(false);
                    return;
                }

                if (licenseResponse.getStatus() == null) {
                    Log.d(TAG, String.format("License response was denied without status"));
                    callback.authenticationCompleted(false);
                    return;
                }

                if (!licenseResponse.getStatus().equals("granted")) {
                    Log.d(TAG, String.format("License response was denied: %s", licenseResponse.getMessage()));
                    callback.authenticationCompleted(false);
                    return;
                }
                Log.d(TAG, "License response was granted");
                callback.authenticationCompleted(true);
            }
        });

    }

}
