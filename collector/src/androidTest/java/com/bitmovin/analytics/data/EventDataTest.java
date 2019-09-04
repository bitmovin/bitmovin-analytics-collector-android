package com.bitmovin.analytics.data;

import android.content.Context;
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

    @Test
    public void serializeTest() throws Exception {
        String expected = "{\"ad\":0,\"analyticsVersion\":\"0.1\",\"audioBitrate\":0,\"buffered\":0,\"cdnProvider\":\"akamai\",\"domain\":\"com.bitmovin.bitmovinanalyticscollector.test\",\"droppedFrames\":0,\"duration\":0,\"impressionId\":\"1234\",\"isCasting\":false,\"isLive\":false,\"isMuted\":false,\"key\":\"9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac\",\"language\":\"en_US\",\"pageLoadTime\":0,\"pageLoadType\":0,\"path\":\"\",\"paused\":0,\"played\":0,\"playerKey\":\"18ca6ad5-9768-4129-bdf6-17685e0d14d2\",\"playerStartupTime\":0,\"playerTech\":\"Native\",\"screenHeight\":2712,\"screenWidth\":1440,\"seeked\":0,\"startupTime\":0,\"time\":0,\"userAgent\":\"Unknown/null (Linux;Android 8.1.0) ExoPlayerLib/2.6.1\",\"userId\":\"0e9717098feb0d7a\",\"videoBitrate\":0,\"videoDuration\":0,\"videoId\":\"video1234\",\"videoPlaybackHeight\":0,\"videoPlaybackWidth\":0,\"videoStartupTime\":0,\"videoTimeEnd\":0,\"videoTimeStart\":0,\"videoWindowHeight\":0,\"videoWindowWidth\":0}";
        Gson gson = new Gson();
        String json = gson.toJson(eventData);

        System.out.println(expected);
        System.out.println(json);
        Assert.assertEquals(json, expected);
    }

    // Test isn't running because I can't get android to compile the apk for the tests :/
    @Test
    public void eventDataContainsDeviceInformation() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();
        BitmovinAnalyticsConfig config = new BitmovinAnalyticsConfig("9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac", "18ca6ad5-9768-4129-bdf6-17685e0d14d2");
        EventData data = new EventData(config, appContext, "1234", "user agent");
        String serialized = DataSerializer.serialize(data);
        assertThat(serialized).contains("\"deviceInformation\":{");
    }
}