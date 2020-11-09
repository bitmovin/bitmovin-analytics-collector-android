package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.features.EventSource;
import com.bitmovin.analytics.features.Feature;

import org.jetbrains.annotations.Nullable;

import java.util.List;


public interface PlayerAdapter extends EventSource<OnPlayerAdapterReleasingEventListener>
{
    EventData createEventData();

    void init();

    void release();

    long getPosition();

    @Nullable
    DRMInformation getDRMInformation();

    void clearValues();

    List<Feature<?>> getFeatures();
}
