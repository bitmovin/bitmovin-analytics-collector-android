package com.bitmovin.bitmovinanalyticscollector.utils;

import com.bitmovin.bitmovinanalyticscollector.data.EventData;
import com.google.gson.Gson;

/**
 * Created by zachmanc on 12/14/17.
 */

public class EventDataSerializer {
    public static String serialize(EventData data){
        Gson gson = new Gson();
        return gson.toJson(data);
    }
}
