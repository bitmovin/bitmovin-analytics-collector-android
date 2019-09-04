package com.bitmovin.analytics.data;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.enums.CDNProvider;
import com.bitmovin.analytics.utils.DataSerializer;
import com.google.gson.Gson;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.*;

@RunWith(AndroidJUnit4.class)
public class EventDataTest {
    private EventData eventData;
    private BitmovinAnalyticsConfig bitmovinAnalyticsConfig;

    @Before
    public void setUp() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();

        bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac", "18ca6ad5-9768-4129-bdf6-17685e0d14d2");
        bitmovinAnalyticsConfig.setCdnProvider(CDNProvider.AKAMAI);
        bitmovinAnalyticsConfig.setVideoId("video1234");

        eventData = new EventData(bitmovinAnalyticsConfig, appContext,"1234", "user agent");
    }

    // Test isn't running because I can't get android to compile the apk for the tests :/
    @Test
    public void eventDataContainsDeviceInformation() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        BitmovinAnalyticsConfig config = new BitmovinAnalyticsConfig("9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac", "18ca6ad5-9768-4129-bdf6-17685e0d14d2");
        EventData data = new EventData(config, appContext, "1234", "user agent");
        String serialized = DataSerializer.serialize(data);
        assertThat(serialized).contains("\"deviceInformation\":{");
        assertThat(serialized).contains(String.format("\"model\":\"%s\"", Build.MODEL));
        assertThat(serialized).contains(String.format("\"manufacturer\":\"%s\"", Build.MANUFACTURER));
    }
}