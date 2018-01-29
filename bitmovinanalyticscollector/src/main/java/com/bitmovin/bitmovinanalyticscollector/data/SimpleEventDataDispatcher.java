package com.bitmovin.bitmovinanalyticscollector.data;

import com.bitmovin.bitmovinanalyticscollector.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.bitmovinanalyticscollector.utils.EventDataSerializer;
import com.bitmovin.bitmovinanalyticscollector.utils.HttpClient;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SimpleEventDataDispatcher implements IEventDataDispatcher {
    private static final String TAG = "SimpleDispatcher";

    private Queue<EventData> data;
    private HttpClient httpClient;
    private boolean enabled = true;

    public SimpleEventDataDispatcher(BitmovinAnalyticsConfig config) {
        this.data = new ConcurrentLinkedQueue<EventData>();
        this.httpClient = new HttpClient(config);
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void add(EventData data) {
//      Currently not using the stack and just sending the data as it comes in. Not sure the format to send multiple messages at a time
//      this.data.add(data);

        if (this.enabled) {
            this.httpClient.post(EventDataSerializer.serialize(data));
        }
    }

    @Override
    public void clear() {
        this.data.clear();
    }

    public List<EventData> pop(int count) {
        ArrayList<EventData> list = new ArrayList<EventData>();
        for (int i = 0; i < count; i++) {
            try {
                list.add(data.remove());
            } catch (EmptyStackException e) {
                break;
            }
        }
        return list;
    }


}
