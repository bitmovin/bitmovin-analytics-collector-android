package com.bitmovin.analytics.api;

import org.junit.Test;

public class SourceMetadataBuilderJavaTest {

    @Test
    public void test_createSourceMetadataWithBuilder() {
        CustomData customData = new CustomData.Builder()
                .setCustomData1("customData1")
                .setCustomData30("customData30")
                .setExperimentName("experimentName")
                .build();

        SourceMetadata sourceMetadata = new SourceMetadata.Builder()
                .setCustomData(customData)
                .setCdnProvider("cdnProvider")
                .setIsLive(true)
                .setTitle("title")
                .setVideoId("videoId")
                .setPath("path")
                .build();

        assert(sourceMetadata.getCustomData().equals(customData));
        assert(sourceMetadata.getCdnProvider().equals("cdnProvider"));
        assert(Boolean.TRUE.equals(sourceMetadata.isLive()));
        assert(sourceMetadata.getTitle().equals("title"));
        assert(sourceMetadata.getVideoId().equals("videoId"));
        assert(sourceMetadata.getPath().equals("path"));
    }
}
