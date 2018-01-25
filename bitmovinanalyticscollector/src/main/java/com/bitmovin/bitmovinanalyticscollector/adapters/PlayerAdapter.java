package com.bitmovin.bitmovinanalyticscollector.adapters;

import com.bitmovin.bitmovinanalyticscollector.data.EventData;

/**
 * Created by zachmanc on 12/15/17.
 */

public interface PlayerAdapter {
    EventData createEventData();

    void release();
}
