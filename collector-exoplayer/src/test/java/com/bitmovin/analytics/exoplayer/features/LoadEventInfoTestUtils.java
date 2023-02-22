package com.bitmovin.analytics.exoplayer.features;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadEventInfoTestUtils {
    // We need to create the ResponseHeaderMap in a java class since
    // this is the only way to insert null key values and pass this a non nullable key map in kotlin
    public static Map<String, List<String>> getResponseHeaderMap (String nullKeyValue){
        Map<String,List<String>> result = new HashMap<>();

        if (nullKeyValue!=null) {
            result.put(null, Arrays.asList(nullKeyValue) );
        }

        // couple of dummy values
        result.put("test", Arrays.asList("HTTP 300 BAD"));
        result.put("test3", Arrays.asList("test value"));
        return result;
    }
}
