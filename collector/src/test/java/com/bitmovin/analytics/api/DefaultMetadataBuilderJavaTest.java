package com.bitmovin.analytics.api;

import org.junit.Test;

import java.util.Objects;

public class DefaultMetadataBuilderJavaTest {

    @Test
    public void test_createDefaultMetadataWithBuilder() {
        CustomData customData = new CustomData.Builder()
                .setCustomData1("customData1")
                .setCustomData30("customData30")
                .setCustomData50("customData50")
                .setExperimentName("experimentName")
                .build();

        DefaultMetadata defaultMetadata = new DefaultMetadata.Builder()
                .setCustomData(customData)
                .setCdnProvider("cdnProvider")
                .setCustomUserId("customUserId")
                .build();

        assert(defaultMetadata.getCustomData().equals(customData));
        assert(Objects.equals(defaultMetadata.getCdnProvider(), "cdnProvider"));
        assert(Objects.equals(defaultMetadata.getCustomUserId(), "customUserId"));
    }
}
