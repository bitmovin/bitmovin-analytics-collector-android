package com.bitmovin.bitmovinanalyticscollector.data;

import com.bitmovin.bitmovinanalyticscollector.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.bitmovinanalyticscollector.utils.HttpClient;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by zachmanc on 12/29/17.
 */

public class SimpleEventDataDispatcher implements IEventDataDispatcher {
    private static final String TAG = "SimpleDispatcher";

    private Queue<EventData> data;
    private HttpClient httpClient;
    private boolean enabled;

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
        Gson gson = new Gson();
        String json = gson.toJson(data);

        this.httpClient.post(json);
    }

    @Override
    public void clear() {
        this.data.clear();
    }

    public List<EventData> pop(int count) {
        ArrayList<EventData> list = new ArrayList<EventData>();
        for (int i=0; i<count; i++){
            try{
                list.add(data.remove());
            }catch (EmptyStackException e){
                break;
            }
        }
        return list;
    }


}
