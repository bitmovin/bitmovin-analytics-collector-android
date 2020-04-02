package com.bitmovin.analytics.adapters;

import com.bitmovin.analytics.data.EventData;

public interface PlayerAdapter {
    EventData createEventData();

    void release();

    long getPosition();
}
