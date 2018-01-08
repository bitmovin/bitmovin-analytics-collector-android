package com.bitmovin.bitmovinanalyticscollector.data;

/**
 * Created by zachmanc on 12/19/17.
 */

public interface IEventDataDispatcher {
    public void enable();
    public void disable();
    public void add(EventData data);
    public void clear();
}
