package com.bitmovin.analytics.amazon.ivs;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.amazon.ivs.api.IAmazonIvsPlayerCollector;

import org.junit.Assert;
import org.junit.Test;

public class IAmazonIvsPlayerCollectorJavaTest {

    // This test is a sanity check that the kotlin factory stays stable and consistent with the naming
    @Test
    public void testFactory_shouldCreateNewCollectorObject(){
        // arrange
        BitmovinAnalyticsConfig config = new BitmovinAnalyticsConfig("test-analytics-key", "test-player-key");
        Context context = TestUtils.Companion.createMockContext();

        // act
        IAmazonIvsPlayerCollector collector = IAmazonIvsPlayerCollector.Factory.create(config, context);
        IAmazonIvsPlayerCollector collector2 = IAmazonIvsPlayerCollector.create(config, context);
        String sdkVersion = IAmazonIvsPlayerCollector.getSdkVersion();

        // assert
        Assert.assertNotNull(collector);
        Assert.assertNotNull(collector2);
        Assert.assertNotNull(sdkVersion);
    }

    @Test
    public void testSdkVersion_shouldReturnVersionString(){
        // act
        String sdkVersion = IAmazonIvsPlayerCollector.getSdkVersion();

        // assert
        Assert.assertNotNull(sdkVersion);
    }
}
