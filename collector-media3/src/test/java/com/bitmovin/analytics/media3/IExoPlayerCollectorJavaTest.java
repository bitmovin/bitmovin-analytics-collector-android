package com.bitmovin.analytics.media3;

import android.content.Context;

import com.bitmovin.analytics.api.AnalyticsConfig;
import com.bitmovin.analytics.api.DefaultMetadata;
import com.bitmovin.analytics.media3.api.IMedia3ExoPlayerCollector;

import org.junit.Assert;
import org.junit.Test;

public class IExoPlayerCollectorJavaTest {

    @Test
    public void testFactory_shouldCreateNewCollectorObjectWithAnalyticsConfig(){
        // arrange
        AnalyticsConfig config = new AnalyticsConfig("test-analytics-key");
        Context context = TestUtils.INSTANCE.createMockContext();
        DefaultMetadata defaultMetadata = new DefaultMetadata();

        // act
        IMedia3ExoPlayerCollector collector = IMedia3ExoPlayerCollector.Factory.create(context, config);
        IMedia3ExoPlayerCollector collector2 = IMedia3ExoPlayerCollector.create(context, config);
        IMedia3ExoPlayerCollector collector3 = IMedia3ExoPlayerCollector.Factory.create(context, config, defaultMetadata);
        IMedia3ExoPlayerCollector collector4 = IMedia3ExoPlayerCollector.create(context, config, defaultMetadata);

        // assert
        Assert.assertNotNull(collector);
        Assert.assertNotNull(collector2);
        Assert.assertNotNull(collector3);
        Assert.assertNotNull(collector4);
    }

    @Test
    public void testSdkVersion_shouldReturnVersionString(){
        // act
        String sdkVersion = IMedia3ExoPlayerCollector.getSdkVersion();

        // assert
        Assert.assertNotNull(sdkVersion);
    }
}
