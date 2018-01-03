package com.bitmovin.bitmovinanalyticscollector.data;

import android.util.Log;

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

    public SimpleEventDataDispatcher() {
        this.data = new Stack<EventData>();
    }

    @Override
    public void push(EventData data) {
        this.data.push(data);
        Log.d(TAG,"Data added");
    }

    @Override
    public void clear() {
        Log.d(TAG,"Data cleared");
        this.data.clear();
    }

    @Override
    public List<EventData> pop(int count) {
        ArrayList<EventData> list = new ArrayList<EventData>();
        for (int i=0; i<count; i++){
            try{
                list.add(data.pop());
            }catch (EmptyStackException e){
                break;
            }
        }

        Log.d(TAG,String.format("Popped %d elements out of the queue",list.size()));
        return list;
    }
}
