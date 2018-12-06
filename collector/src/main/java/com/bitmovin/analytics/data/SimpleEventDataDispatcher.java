package com.bitmovin.analytics.data;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.utils.DataSerializer;
import com.bitmovin.analytics.utils.HttpClient;
import com.bitmovin.analytics.utils.LicenseCall;
import com.bitmovin.analytics.utils.LicenseCallback;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpleEventDataDispatcher implements IEventDataDispatcher, LicenseCallback {
    private static final String TAG = "SimpleDispatcher";

    private Queue<EventData> data;
    private HttpClient httpClient;
    private boolean enabled = false;
    private BitmovinAnalyticsConfig config;
    private LicenseCallback callback;

    private int sampleSequenceNumber = 0;

    public SimpleEventDataDispatcher(BitmovinAnalyticsConfig config, LicenseCallback callback) {
        this.data = new ConcurrentLinkedQueue<EventData>();
        this.httpClient = new HttpClient(config.getContext(), BitmovinAnalyticsConfig.analyticsUrl);
        this.config = config;
        this.callback = callback;
    }

    @Override
    synchronized public void authenticationCompleted(boolean success) {
        if (success) {
            enabled = true;
            Iterator<EventData> it = data.iterator();
            while (it.hasNext()) {
                EventData eventData = it.next();
                this.httpClient.post(DataSerializer.serialize(eventData), null);
                it.remove();
            }
        }

        if(callback != null) {
            callback.authenticationCompleted(success);
        }
    }

    @Override
    public void enable() {
        LicenseCall licenseCall = new LicenseCall(config);
        licenseCall.authenticate(this);
    }

    @Override
    public void disable() {
        this.data.clear();
        this.enabled = false;
        this.sampleSequenceNumber = 0;
    }

    @Override
    public void add(EventData eventData) {
        eventData.setSequenceNumber(this.sampleSequenceNumber++);

        if (enabled) {
            this.httpClient.post(DataSerializer.serialize(eventData), null);
        } else {
            this.data.add(eventData);
        }
    }

    @Override
    public void clear() {
        this.data.clear();
    }

}
