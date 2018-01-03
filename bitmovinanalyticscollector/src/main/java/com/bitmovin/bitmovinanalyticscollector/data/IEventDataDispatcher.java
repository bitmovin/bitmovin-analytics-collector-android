package com.bitmovin.bitmovinanalyticscollector.data;

import java.util.List;

/**
 * Created by zachmanc on 12/19/17.
 */

public interface IEventDataDispatcher {


    public void push(EventData data);
    public void clear();
    public List<EventData> pop(int count);
}
