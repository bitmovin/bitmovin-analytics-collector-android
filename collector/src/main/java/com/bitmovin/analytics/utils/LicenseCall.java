package com.bitmovin.analytics.utils;

import android.content.Context;
import android.util.Log;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.LicenseCallData;
import com.bitmovin.analytics.data.LicenseResponse;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LicenseCall {
    private static final String TAG = "BitmovinAnalytics";
    private BitmovinAnalyticsConfig config;
    private Context context;
    private HttpClient httpClient;

    public LicenseCall(BitmovinAnalyticsConfig config, Context context) {
        this.config = config;
        this.context = context;
        this.httpClient = new HttpClient(context, config.getLicenseUrl());
    }

    public void authenticate(final LicenseCallback callback) {
        LicenseCallData data = new LicenseCallData();
        data.setKey(config.getKey());
        data.setAnalyticsVersion(Util.getVersion());
        data.setDomain(context.getPackageName());
        String json = DataSerializer.serialize(data);
        httpClient.post(json, new Callback() {
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

                LicenseResponse licenseResponse = DataSerializer.deserialize(response.body().string(), LicenseResponse.class);
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
