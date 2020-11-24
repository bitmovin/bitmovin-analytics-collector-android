package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.data.DRMInformation;
import com.bitmovin.analytics.data.EventData;
import com.bitmovin.analytics.data.EventDataDecorator;

import org.jetbrains.annotations.Nullable;


public interface PlayerAdapter extends EventDataDecorator {

    void init();

    void release();

    long getPosition();

    @Nullable
    DRMInformation getDRMInformation();

    void clearValues();
}
