package com.bitmovin.analytics.theoplayer;

import android.content.Context;

import com.bitmovin.analytics.api.AnalyticsConfig;
import com.bitmovin.analytics.api.DefaultMetadata;
import com.bitmovin.analytics.theoplayer.api.ITHEOplayerCollector;

import org.junit.Assert;
import org.junit.Test;

public class ITHEOplayerCollectorJavaTest {

    @Test
    public void testFactory_shouldCreateNewCollectorObjectWithAnalyticsConfig(){
        // arrange
        AnalyticsConfig config = new AnalyticsConfig("test-analytics-key");
        Context context = TestUtils.INSTANCE.createMockContext();
        DefaultMetadata defaultMetadata = new DefaultMetadata();

        // act
        ITHEOplayerCollector collector = ITHEOplayerCollector.Factory.create(context, config);
        ITHEOplayerCollector collector2 = ITHEOplayerCollector.create(context, config);
        ITHEOplayerCollector collector3 = ITHEOplayerCollector.Factory.create(context, config, defaultMetadata);
        ITHEOplayerCollector collector4 = ITHEOplayerCollector.create(context, config, defaultMetadata);

        // assert
        Assert.assertNotNull(collector);
        Assert.assertNotNull(collector2);
        Assert.assertNotNull(collector3);
        Assert.assertNotNull(collector4);
    }

    @Test
    public void testSdkVersion_shouldReturnVersionString(){
        // act
        String sdkVersion = ITHEOplayerCollector.getSdkVersion();

        // assert
        Assert.assertNotNull(sdkVersion);
    }
}