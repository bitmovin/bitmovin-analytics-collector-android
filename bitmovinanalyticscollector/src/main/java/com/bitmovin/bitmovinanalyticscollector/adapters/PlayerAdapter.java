package com.bitmovin.bitmovinanalyticscollector.adapters;

import com.bitmovin.bitmovinanalyticscollector.data.EventData;

public interface PlayerAdapter {
    EventData createEventData();

    void release();
}
