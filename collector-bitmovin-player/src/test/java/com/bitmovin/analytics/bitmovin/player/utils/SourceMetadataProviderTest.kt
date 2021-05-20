package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import org.assertj.core.api.Assertions
import org.junit.Test

class SourceMetadataProviderTest {

    @Test
    fun `sourceMetadataProvider sets and gets sourceMetadata`() {
        // arrange
        val sourceMetadataProvider = SourceMetadataProvider()
        val testSource = Source.create(SourceConfig.fromUrl("https://bitmovin-a.akamaihd.net/content/sintel/sintel.mpd"))
        val testSourceMetadata = SourceMetadata(
                videoId = "source-video-id",
                title = "test",
                customData1 = "someTestData",
                customData2 = "someTestData2",
                experimentName = "testExperiment"
        )

        // act
        sourceMetadataProvider.setSourceMetadata(testSource, testSourceMetadata)

        // asset
        val returnedSource = sourceMetadataProvider.getSourceMetadata(testSource)
        Assertions.assertThat(returnedSource).isEqualTo(testSourceMetadata)
    }

    @Test
    fun `sourceMetadataProvider replaces existing sourceMetadata`() {
        // arrange
        val sourceMetadataProvider = SourceMetadataProvider()
        val testSource = Source.create(SourceConfig.fromUrl("https://bitmovin-a.akamaihd.net/content/sintel/sintel.mpd"))
        val testSourceMetadata = SourceMetadata(
                videoId = "source-video-id",
                title = "test",
                customData1 = "someTestData",
                customData2 = "someTestData2",
                experimentName = "testExperiment"
        )
        sourceMetadataProvider.setSourceMetadata(testSource, testSourceMetadata)

        // act
        val newTestSourceMetadata = SourceMetadata(
                videoId = "new-video-id",
                title = "new test",
                customData1 = "new data1",
                customData2 = "new data1",
                experimentName = "new experiment"
        )
        sourceMetadataProvider.setSourceMetadata(testSource, newTestSourceMetadata)

        // asset
        val returnedSource = sourceMetadataProvider.getSourceMetadata(testSource)
        Assertions.assertThat(returnedSource).isEqualTo(newTestSourceMetadata)
    }
}
