package com.bitmovin.analytics.bitmovin.player;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.api.AnalyticsConfig;
import com.bitmovin.analytics.api.DefaultMetadata;
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector;

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
        AnalyticsConfig analyticsConfig = new AnalyticsConfig("analytics_key");
        Context context = TestUtils.Companion.createMockContext();
        DefaultMetadata defaultMetadata = new DefaultMetadata();

        IBitmovinPlayerCollector collector = IBitmovinPlayerCollector.Factory.create(context, analyticsConfig, defaultMetadata);
        Assert.assertNotNull(collector);

        IBitmovinPlayerCollector collector2 = IBitmovinPlayerCollector.create(context, analyticsConfig, defaultMetadata);
        Assert.assertNotNull(collector2);

        IBitmovinPlayerCollector collector3 = IBitmovinPlayerCollector.Factory.create(context, analyticsConfig);
        Assert.assertNotNull(collector3);

        IBitmovinPlayerCollector collector4 = IBitmovinPlayerCollector.create(context, analyticsConfig);
        Assert.assertNotNull(collector4);
    }

    @Test
    public void testSdkVersion_shouldReturnVersionString(){
        // act
        String sdkVersion = IBitmovinPlayerCollector.getSdkVersion();

        // assert
        Assert.assertNotNull(sdkVersion);
    }
}
