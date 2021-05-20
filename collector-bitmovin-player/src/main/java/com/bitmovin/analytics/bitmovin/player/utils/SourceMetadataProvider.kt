package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.player.api.source.Source

class SourceMetadataProvider {
    companion object {
        val sourceMetadataMap = HashMap<Source, SourceMetadata>()
    }

    fun setSourceMetadata(playerSource: Source, sourceMetadata: SourceMetadata) {
        if (sourceMetadataMap.containsKey(playerSource)) {
            sourceMetadataMap.remove(playerSource)
        }
        sourceMetadataMap[playerSource] = sourceMetadata
    }

    fun getSourceMetadata(playerSource: Source): SourceMetadata? {
        return sourceMetadataMap[playerSource]
    }
}
