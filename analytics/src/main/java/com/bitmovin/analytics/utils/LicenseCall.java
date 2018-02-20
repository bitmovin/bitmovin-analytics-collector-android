package com.bitmovin.analytics.utils;

import com.bitmovin.analytics.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.data.LicenseCallData;
import com.bitmovin.analytics.data.LicenseResponse;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LicenseCall {
    private static final String TAG = "LicenseCall";
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
                callback.authenticationCompleted(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response != null) {
                    LicenseResponse licenseResponse = DataSerializer.deserialize(response.body().string(), LicenseResponse.class);
                    if (licenseResponse != null && licenseResponse.getStatus().equals("granted")) {
                        callback.authenticationCompleted(true);
                        return;
                    }
                }
                callback.authenticationCompleted(false);
            }
        });

    }

}
