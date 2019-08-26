package com.bitmovin.analytics.data;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.license.LicenseProvider;
import com.bitmovin.analytics.license.LicenseCall;
import com.bitmovin.analytics.license.OnLicenseValidated;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpleEventDataDispatcher implements IEventDataDispatcher, OnLicenseValidated {
    private static final String TAG = "BitmovinAnalytics/SimpleDispatcher";
    private final LicenseProvider licenseProvider;
    private final Backend backend;

    private Queue<EventData> data;

    private boolean enabled = false;
    private BitmovinAnalyticsConfig config;
    private OnLicenseValidated callback;
    private Context context;

    private int sampleSequenceNumber = 0;

    public SimpleEventDataDispatcher(BitmovinAnalyticsConfig config, Context context, OnLicenseValidated callback, LicenseProvider licenseProvider) {
        this.licenseProvider = licenseProvider;
        this.data = new ConcurrentLinkedQueue<EventData>();
        this.config = config;
        this.callback = callback;
        this.context = context;
        this.backend = new HttpBackend(config, context);
    }

    @Override
    synchronized public void validationCompleted(boolean success, String key) {
        if (success) {
            enabled = true;
            Iterator<EventData> it = data.iterator();
            while (it.hasNext()) {
                EventData eventData = it.next();
                eventData.setKey(licenseProvider.getAnalyticsLicense());
                this.backend.send(eventData);
                it.remove();
            }
        }

        if(callback != null) {
            callback.validationCompleted(success, key);
        }
    }

    @Override
    public void enable() {
        LicenseCall licenseCall = new LicenseCall(config, context, licenseProvider);
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
        eventData.setKey(licenseProvider.getAnalyticsLicense());
        if (enabled) {
            this.backend.send(eventData);
        } else {
            this.data.add(eventData);
        }
    }

    @Override
    public void clear() {
        this.data.clear();
    }

}
