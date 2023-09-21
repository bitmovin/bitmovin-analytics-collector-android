package com.bitmovin.analytics.amazon.ivs;

import android.content.Context;

import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import com.bitmovin.analytics.amazon.ivs.api.IAmazonIvsPlayerCollector;
import com.bitmovin.analytics.api.AnalyticsConfig;
import com.bitmovin.analytics.api.DefaultMetadata;

import org.junit.Assert;
import org.junit.Test;

public class IAmazonIvsPlayerCollectorJavaTest {

    // This test is a sanity check that the kotlin factory stays stable and consistent with the naming
    @Test
    public void testFactory_shouldCreateNewCollectorObjectForAnalyticsConfig(){
        // arrange
        AnalyticsConfig config = new AnalyticsConfig("test-analytics-key");
        Context context = TestUtils.INSTANCE.createMockContext();
        DefaultMetadata defaultMetadata = new DefaultMetadata();

        // act
        IAmazonIvsPlayerCollector collector = IAmazonIvsPlayerCollector.Factory.create(context, config);
        IAmazonIvsPlayerCollector collector2 = IAmazonIvsPlayerCollector.create(context, config);
        IAmazonIvsPlayerCollector collector3 = IAmazonIvsPlayerCollector.Factory.create(context, config, defaultMetadata);
        IAmazonIvsPlayerCollector collector4 = IAmazonIvsPlayerCollector.create(context, config, defaultMetadata);

        // assert
        Assert.assertNotNull(collector);
        Assert.assertNotNull(collector2);
        Assert.assertNotNull(collector3);
        Assert.assertNotNull(collector4);
    }

    @Test
    public void testSdkVersion_shouldReturnVersionString(){
        // act
        String sdkVersion = IAmazonIvsPlayerCollector.getSdkVersion();

        // assert
        Assert.assertNotNull(sdkVersion);
    }
}
