package com.bitmovin.analytics.exoplayer;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;

import org.junit.Assert;
import org.junit.Test;

public class IExoPlayerCollectorJavaTest {

    // This test is a sanity check that the kotlin factory stays stable and consistent with the naming
    @Test
    public void testFactory_shouldCreateNewCollectorObject(){
        // arrange
        BitmovinAnalyticsConfig config = new BitmovinAnalyticsConfig("test-analytics-key", "test-player-key");
        Context context = TestUtils.Companion.createMockContext();

        // act
        IExoPlayerCollector collector = IExoPlayerCollector.Factory.create(config, context);
        IExoPlayerCollector collector2 = IExoPlayerCollector.create(config, context);

        // assert
        Assert.assertNotNull(collector);
        Assert.assertNotNull(collector2);
    }
}
