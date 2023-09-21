package com.bitmovin.analytics.exoplayer;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.api.AnalyticsConfig;
import com.bitmovin.analytics.api.DefaultMetadata;
import com.bitmovin.analytics.exoplayer.api.IExoPlayerCollector;

import org.junit.Assert;
import org.junit.Test;

public class IExoPlayerCollectorJavaTest {

    // This test is a sanity check that the kotlin factory stays stable and consistent with the naming
    @Test
    public void testFactory_shouldCreateNewCollectorObject(){
        // arrange
        BitmovinAnalyticsConfig config = new BitmovinAnalyticsConfig("test-analytics-key", "test-player-key");
        Context context = TestUtils.INSTANCE.createMockContext();
        DefaultMetadata defaultMetadata = new DefaultMetadata();

        // act
        IExoPlayerCollector collector = IExoPlayerCollector.Factory.create(config, context);
        IExoPlayerCollector collector2 = IExoPlayerCollector.create(config, context);
        IExoPlayerCollector collector3 = IExoPlayerCollector.Factory.create(config, context);
        IExoPlayerCollector collector4 = IExoPlayerCollector.create(config, context);

        // assert
        Assert.assertNotNull(collector);
        Assert.assertNotNull(collector2);
    }

    @Test
    public void testFactory_shouldCreateNewCollectorObjectWithAnalyticsConfig(){
        // arrange
        AnalyticsConfig config = new AnalyticsConfig("test-analytics-key");
        Context context = TestUtils.INSTANCE.createMockContext();
        DefaultMetadata defaultMetadata = new DefaultMetadata();

        // act
        IExoPlayerCollector collector = IExoPlayerCollector.Factory.create(context, config);
        IExoPlayerCollector collector2 = IExoPlayerCollector.create(context, config);
        IExoPlayerCollector collector3 = IExoPlayerCollector.Factory.create(context, config, defaultMetadata);
        IExoPlayerCollector collector4 = IExoPlayerCollector.create(context, config, defaultMetadata);

        // assert
        Assert.assertNotNull(collector);
        Assert.assertNotNull(collector2);
        Assert.assertNotNull(collector3);
        Assert.assertNotNull(collector4);
    }

    @Test
    public void testSdkVersion_shouldReturnVersionString(){
        // act
        String sdkVersion = IExoPlayerCollector.getSdkVersion();

        // assert
        Assert.assertNotNull(sdkVersion);
    }
}
