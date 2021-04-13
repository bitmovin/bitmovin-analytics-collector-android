package com.bitmovin.analytics.bitmovin.player.config

import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.player.api.source.Source

class BitmovinSourceMetadataProvider {
    private val sources = mutableMapOf<Source, SourceMetadata>()

    fun addSource(playerSource: Source, sourceMetadata: SourceMetadata) {
        sources[playerSource] = sourceMetadata
    }

    fun getSource(playerSource: Source?): SourceMetadata? {
        if (playerSource == null) {
            return null
        }
        return sources[playerSource]
    }
}
