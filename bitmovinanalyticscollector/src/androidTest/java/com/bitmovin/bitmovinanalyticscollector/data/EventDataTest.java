package com.bitmovin.bitmovinanalyticscollector.data;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.bitmovin.bitmovinanalyticscollector.enums.CDNProvider;
import com.bitmovin.bitmovinanalyticscollector.enums.PlayerType;
import com.bitmovin.bitmovinanalyticscollector.utils.BitmovinAnalyticsConfig;
import com.google.gson.Gson;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by zachmanc on 12/15/17.
 */
@RunWith(AndroidJUnit4.class)
public class EventDataTest {
    private EventData eventData;
    private BitmovinAnalyticsConfig bitmovinAnalyticsConfig;
    @Before
    public void setUp() throws Exception {
        Context appContext = InstrumentationRegistry.getTargetContext();

        bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac", "18ca6ad5-9768-4129-bdf6-17685e0d14d2", appContext);
        bitmovinAnalyticsConfig.setCdnProvider(CDNProvider.AKAMAI);
        bitmovinAnalyticsConfig.setPlayerType(PlayerType.EXOPLAYER);
        bitmovinAnalyticsConfig.setVideoId("video1234");
        bitmovinAnalyticsConfig.setUserId("user1234");

        eventData = new EventData(bitmovinAnalyticsConfig);
    }

    @Test
    public void serializeTest() throws Exception {
        String expected = "{\"ad\":false,\"analyticsVersion\":\"0.1\",\"audioBitrate\":0,\"buffered\":0,\"cdnProvider\":\"akamai\",\"domain\":\"com.bitmovin.bitmovinanalyticscollector.test\",\"droppedFrames\":0,\"duration\":0,\"isCasting\":false,\"isLive\":false,\"isMuted\":false,\"key\":\"9ae0b480-f2ee-4c10-bc3c-cb88e982e0ac\",\"language\":\"en_US\",\"pageLoadTime\":0,\"pageLoadType\":0,\"paused\":0,\"played\":0,\"player\":\"exoplayer\",\"playerKey\":\"18ca6ad5-9768-4129-bdf6-17685e0d14d2\",\"playerStartupTime\":0,\"playerTech\":\"Native\",\"screenHeight\":1794,\"screenWidth\":1080,\"seeked\":0,\"startupTime\":0,\"time\":0,\"userId\":\"user1234\",\"videoBitrate\":0,\"videoDuration\":0,\"videoId\":\"video1234\",\"videoPlaybackHeight\":0,\"videoPlaybackWidth\":0,\"videoStartupTime\":0,\"videoTimeEnd\":0,\"videoTimeStart\":0,\"videoWindowHeight\":0,\"videoWindowWidth\":0}";
        Gson gson = new Gson();
        String json = gson.toJson(eventData);

        System.out.println(expected);
        System.out.println(json);
        Assert.assertEquals(json, expected);
    }
}