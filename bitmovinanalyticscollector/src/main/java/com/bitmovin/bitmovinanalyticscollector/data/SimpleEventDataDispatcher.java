package com.bitmovin.bitmovinanalyticscollector.data;

import android.util.Log;

import com.bitmovin.bitmovinanalyticscollector.utils.BitmovinAnalyticsConfig;
import com.bitmovin.bitmovinanalyticscollector.utils.HttpClient;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

/**
 * Created by zachmanc on 12/29/17.
 */

public class SimpleEventDataDispatcher implements IEventDataDispatcher {
    private static final String TAG = "SimpleDispatcher";

    private Stack<EventData> data;
    private HttpClient httpClient;

    public SimpleEventDataDispatcher(BitmovinAnalyticsConfig config) {
        this.data = new Stack<EventData>();
        this.httpClient = new HttpClient(config);
    }

    @Override
    public void add(EventData data) {
        // Currently not using the stack and just sending the data as it comes in. Not sure the format to send multiple messages at a time
        this.data.push(data);
        Gson gson = new Gson();
        String json = gson.toJson(data);
        Log.d(TAG,json);

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
                list.add(data.pop());
            }catch (EmptyStackException e){
                break;
            }
        }
        return list;
    }
}
