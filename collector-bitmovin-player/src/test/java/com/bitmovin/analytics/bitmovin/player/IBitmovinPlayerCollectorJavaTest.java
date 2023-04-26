package com.bitmovin.analytics.bitmovin.player;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.api.AnalyticsConfig;

import org.junit.Assert;
import org.junit.Test;

public class IBitmovinPlayerCollectorJavaTest {

    // This test is a sanity check that the kotlin factory stays stable and consistent with the naming
    @Test
    public void testFactory_shouldCreateNewCollectorObject() {
        // arrange
        BitmovinAnalyticsConfig config = new BitmovinAnalyticsConfig("test-analytics-key", "test-player-key");
        Context context = TestUtils.Companion.createMockContext();

        // act
        IBitmovinPlayerCollector collector = IBitmovinPlayerCollector.Factory.create(config, context);
        IBitmovinPlayerCollector collector2 = IBitmovinPlayerCollector.create(config, context);

        // assert
        Assert.assertNotNull(collector);
        Assert.assertNotNull(collector2);
    }

    @Test
    public void testFactory_shouldCreateNewCollectorObjectWithAnalyticsConfig() {
        AnalyticsConfig analyticsConfig = new AnalyticsConfig("analytics_key", "player_key");
        Context context = TestUtils.Companion.createMockContext();

        IBitmovinPlayerCollector collector = IBitmovinPlayerCollector.Factory.create(analyticsConfig, context);
        Assert.assertNotNull(collector);
        Assert.assertEquals(analyticsConfig.getKey(), collector.getConfig().getKey());
        Assert.assertEquals(analyticsConfig.getPlayerKey(), collector.getConfig().getPlayerKey());

        IBitmovinPlayerCollector collector2 = IBitmovinPlayerCollector.create(analyticsConfig, context);
        Assert.assertNotNull(collector2);
        Assert.assertEquals(analyticsConfig.getKey(), collector2.getConfig().getKey());
        Assert.assertEquals(analyticsConfig.getPlayerKey(), collector2.getConfig().getPlayerKey());
    }
}
