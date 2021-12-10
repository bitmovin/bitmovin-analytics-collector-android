package com.bitmovin.analytics.exoplayer;

import android.content.Context;
import com.bitmovin.analytics.BitmovinAnalyticsConfig;
import org.junit.Test;
import org.mockito.Mockito;

public class ExoPlayerCollectorJavaTest
{
    @Test
    public void testDeprecatedConstructorSucceedsWithValidContext() {
        BitmovinAnalyticsConfig config =
                new BitmovinAnalyticsConfig("", Mockito.mock(Context.class));
        ExoPlayerCollector collector = new ExoPlayerCollector(config);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeprecatedConstructorChecksForNullInConfiguration() {
        BitmovinAnalyticsConfig config = new BitmovinAnalyticsConfig("");
        ExoPlayerCollector collector = new ExoPlayerCollector(config);
    }
}
