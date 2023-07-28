package com.bitmovin.analytics.api;

import org.junit.Test;

public class AnalyticsConfigBuilderJavaTest {

    @Test
    public void test_createAnalyticsConfigWithBuilder() {
        AnalyticsConfig analyticsConfig = new AnalyticsConfig.Builder("key")
                .setAdTrackingDisabled(true)
                .setRetryPolicy(RetryPolicy.LONG_TERM)
                .setRandomizeUserId(true)
                .setBackendUrl("https://example.com")
                .build();

        assert(analyticsConfig.getAdTrackingDisabled());
        assert(analyticsConfig.getRetryPolicy() == RetryPolicy.LONG_TERM);
        assert(analyticsConfig.getRandomizeUserId());
        assert(analyticsConfig.getBackendUrl().equals("https://example.com"));
        assert(analyticsConfig.getLicenseKey().equals("key"));
    }

    @Test
    public void test_createMinimalAnalyticsConfig_shouldHaveDefaultValues() {
        AnalyticsConfig analyticsConfig = new AnalyticsConfig.Builder("key")
                .build();

        assert(analyticsConfig.equals(new AnalyticsConfig("key")));
    }
}
