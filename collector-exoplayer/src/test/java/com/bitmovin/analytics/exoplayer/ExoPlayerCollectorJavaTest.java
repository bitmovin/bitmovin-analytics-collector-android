package com.bitmovin.analytics.exoplayer;

import static org.mockito.ArgumentMatchers.any;

import android.content.Context;
import org.junit.Test;
import org.mockito.Mockito;

public class ExoPlayerCollectorJavaTest {
    @Test
    public void testDeprecatedConstructorSucceedsWithValidContext() {
        Mockito.mockStatic(com.google.android.exoplayer2.util.Util.class)
                .when(() -> com.google.android.exoplayer2.util.Util.getUserAgent(any(), any()))
                .thenReturn("");

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
