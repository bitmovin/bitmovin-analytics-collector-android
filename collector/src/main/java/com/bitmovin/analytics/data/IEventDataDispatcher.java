package com.bitmovin.analytics.data;

public interface IEventDataDispatcher {
    public void enable();

    public void disable();

    public void add(EventData data);

    public void addAd(AdEventData data);

    public void resetSourceRelatedState();
}
