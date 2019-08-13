package com.bitmovin.analytics.data;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.license.Licenser;
import com.bitmovin.analytics.utils.DataSerializer;
import com.bitmovin.analytics.utils.HttpClient;
import com.bitmovin.analytics.license.LicenseCall;
import com.bitmovin.analytics.license.OnAuthCompleted;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpleEventDataDispatcher implements IEventDataDispatcher, OnAuthCompleted {
    private static final String TAG = "SimpleDispatcher";
    private final Licenser licenser;

    private Queue<EventData> data;
    private HttpClient httpClient;
    private boolean enabled = false;
    private BitmovinAnalyticsConfig config;
    private OnAuthCompleted callback;
    private Context context;

    private int sampleSequenceNumber = 0;

    public SimpleEventDataDispatcher(BitmovinAnalyticsConfig config, Context context, OnAuthCompleted callback, Licenser licenser) {
        this.licenser = licenser;
        this.data = new ConcurrentLinkedQueue<EventData>();
        this.httpClient = new HttpClient(context, config.getAnalyticsUrl());
        this.config = config;
        this.callback = callback;
        this.context = context;
    }

    @Override
    synchronized public void authenticationCompleted(boolean success, String key) {
        if (success) {
            enabled = true;
            Iterator<EventData> it = data.iterator();
            while (it.hasNext()) {
                EventData eventData = it.next();
                eventData.setKey(licenser.getAnalyticsLicense());
                this.httpClient.post(DataSerializer.serialize(eventData), null);
                it.remove();
            }
        }

        if(callback != null) {
            callback.authenticationCompleted(success, key);
        }
    }

    @Override
    public void enable() {
        LicenseCall licenseCall = new LicenseCall(config, context, licenser);
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
        eventData.setKey(licenser.getAnalyticsLicense());
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
