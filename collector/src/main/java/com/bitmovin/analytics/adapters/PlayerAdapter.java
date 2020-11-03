package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.EventData;

import org.jetbrains.annotations.Nullable;


public interface PlayerAdapter {
    EventData createEventData();

    void init();

    void release();

    long getPosition();

    @Nullable
    DRMInformation getDRMInformation();

    void clearValues();

    <TAdapter> TAdapter getFeatureAdapter(Class<TAdapter> adapterClass);
}
