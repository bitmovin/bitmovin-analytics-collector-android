package com.bitmovin.analytics.utils;

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
    private HttpClient httpClient;

    public LicenseCall(BitmovinAnalyticsConfig config) {
        this.config = config;
        this.httpClient = new HttpClient(config.getContext(), BitmovinAnalyticsConfig.licenseUrl);
    }

    public void authenticate(final LicenseCallback callback) {
        LicenseCallData data = new LicenseCallData();
        data.setKey(config.getKey());
        data.setAnalyticsVersion(Util.getVersion());
        data.setDomain(config.getContext().getPackageName());
        String json = DataSerializer.serialize(data);
        httpClient.post(json, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "License call was denied", e);
                callback.authenticationCompleted(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null) {
                    LicenseResponse licenseResponse = DataSerializer.deserialize(response.body().string(), LicenseResponse.class);
                    if (licenseResponse != null && licenseResponse.getStatus().equals("granted")) {
                        Log.d(TAG, "License response was granted");
                        callback.authenticationCompleted(true);
                        return;
                    }
                    Log.d(TAG, String.format("License response was denied: %s", licenseResponse.getMessage()));
                }
                Log.d(TAG, "License call was denied without providing a response body");
                callback.authenticationCompleted(false);
            }
        });

    }

}
