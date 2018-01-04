package com.bitmovin.bitmovinanalyticscollector.data;

import java.util.List;

/**
 * Created by zachmanc on 12/19/17.
 */

public interface IEventDataDispatcher {


    public void add(EventData data);
    public void clear();
}
