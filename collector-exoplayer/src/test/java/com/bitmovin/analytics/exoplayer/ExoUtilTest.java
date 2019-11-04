package com.bitmovin.analytics.exoplayer;

import com.bitmovin.analytics.CollectorConfig;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExoUtilTest {
    @Test
    public void testGetIsLiveFromConfigOrPlayer_ReturnsConfigValueTrueIfPlayerNotReady() throws Exception {
        CollectorConfig config = new CollectorConfig();
        config.setIsLive(true);

        boolean isLive = ExoUtil.getIsLiveFromConfigOrPlayer(false, config, false);
        assertTrue(isLive);
    }
    @Test
    public void testGetIsLiveFromConfigOrPlayer_ReturnsConfigValueFalseIfPlayerNotReady() throws Exception {
        CollectorConfig config = new CollectorConfig();
        config.setIsLive(false);

        boolean isLive = ExoUtil.getIsLiveFromConfigOrPlayer(false, config, false);
        assertFalse(isLive);
    }

    @Test
    public void testGetIsLiveFromConfigOrPlayer_ReturnsPlayerIsLiveTrueIfPlayerReady() throws Exception {
        CollectorConfig config = new CollectorConfig();
        config.setIsLive(true);

        boolean isLive = ExoUtil.getIsLiveFromConfigOrPlayer(true, config, true);
        assertTrue(isLive);
    }
    @Test
    public void testGetIsLiveFromConfigOrPlayer_ReturnsPlayerIsLiveFalseIfPlayerReady() throws Exception {
        CollectorConfig config = new CollectorConfig();
        config.setIsLive(true);

        boolean isLive = ExoUtil.getIsLiveFromConfigOrPlayer(true, config, false);
        assertFalse(isLive);
    }
}