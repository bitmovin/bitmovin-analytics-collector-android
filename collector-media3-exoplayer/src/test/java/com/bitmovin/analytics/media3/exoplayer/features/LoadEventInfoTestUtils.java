package com.bitmovin.analytics.media3.exoplayer.features;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// We need to create the ResponseHeaderMap in a java class since
// this is the only way to insert null key values and pass this as a non nullable key map in kotlin
public class LoadEventInfoTestUtils {
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
