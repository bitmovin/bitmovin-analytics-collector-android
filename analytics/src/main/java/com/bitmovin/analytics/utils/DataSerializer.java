package com.bitmovin.analytics.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class DataSerializer {
    public static <T> String serialize(T data) {
        Gson gson = new Gson();
        return gson.toJson(data);
    }

    public static <T> T deserialize(String input, Class<T> classOfT) {
        T response = null;
        try {
            response = new Gson().fromJson(input, classOfT);
            return response;
        } catch (JsonSyntaxException e) {
            return response;
        }

    }
}
