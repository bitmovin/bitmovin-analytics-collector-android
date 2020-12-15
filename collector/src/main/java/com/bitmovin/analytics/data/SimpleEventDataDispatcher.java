package com.bitmovin.analytics.data;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.CollectorConfig;
import com.bitmovin.analytics.license.LicenseCall;
import com.bitmovin.analytics.license.LicenseCallback;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpleEventDataDispatcher implements IEventDataDispatcher, LicenseCallback {
    private static final String TAG = "BitmovinAnalytics/SimpleDispatcher";
    private final Backend backend;

    private Queue<EventData> data;
    private Queue<AdEventData> adData;

    private boolean enabled = false;
    private CollectorConfig config;
    private final LicenseCallback callback;
    private Context context;

    private int sampleSequenceNumber = 0;

    public SimpleEventDataDispatcher(CollectorConfig config, Context context, LicenseCallback callback) {
        this.data = new ConcurrentLinkedQueue<>();
        this.adData = new ConcurrentLinkedQueue<>();
        this.config = config;
        this.callback = callback;
        this.context = context;
        this.backend =  new BackendFactory().createBackend(config, context);
    }

    @Override
    synchronized public void authenticationCompleted(boolean success) {
        if (success) {
            enabled = true;
            Iterator<EventData> it = data.iterator();
            while (it.hasNext()) {
                EventData eventData = it.next();
                this.backend.send(eventData);
                it.remove();
            }
            Iterator<AdEventData> adIt = adData.iterator();
            while (adIt.hasNext()) {
                AdEventData eventData = adIt.next();
                this.backend.sendAd(eventData);
                adIt.remove();
            }
        }

        if(callback != null) {
            callback.authenticationCompleted(success);
        }
    }

    @Override
    public void enable() {
        LicenseCall licenseCall = new LicenseCall(config, context);
        licenseCall.authenticate(this);
    }

    @Override
    public void disable() {
        this.data.clear();
        this.adData.clear();
        this.enabled = false;
        this.sampleSequenceNumber = 0;
    }

    @Override
    public void add(EventData eventData) {
        eventData.setSequenceNumber(this.sampleSequenceNumber++);
        if (enabled) {
            this.backend.send(eventData);
        } else {
            this.data.add(eventData);
        }
    }

    @Override
    public void addAd(AdEventData eventData) {
        if (enabled) {
            this.backend.sendAd(eventData);
        } else {
            this.adData.add(eventData);
        }
    }

    @Override
    public void clear() {
        this.data.clear();
        this.adData.clear();
    }

}
