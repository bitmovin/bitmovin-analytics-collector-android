package com.bitmovin.analytics;

import org.junit.Test;

public class BitmovinAnalyticsTest {

    @Test(expected = IllegalArgumentException.class)
    public void testDeprecatedConstructorChecksForNullInConfiguration() {
        BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("foo-bar");
        new BitmovinAnalytics(bitmovinAnalyticsConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewDefaultConstructorChecksForNull() {
        BitmovinAnalyticsConfig bitmovinAnalyticsConfig = new BitmovinAnalyticsConfig("foo-bar");
        new BitmovinAnalytics(bitmovinAnalyticsConfig, null);
    }
}
