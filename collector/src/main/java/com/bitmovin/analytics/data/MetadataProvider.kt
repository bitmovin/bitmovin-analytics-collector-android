package com.bitmovin.analytics.data

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.utils.ApiV3Utils
import java.util.concurrent.ConcurrentHashMap

class MetadataProvider {
    private val sourceMetadataMap = ConcurrentHashMap<Any, SourceMetadata?>()

    // TODO: we should probably use thread save references for default metadata and deprecated bitmovin analytics config
    var deprecatedBitmovinAnalyticsConfig: BitmovinAnalyticsConfig? = null

    fun setSourceMetadata(source: Any, sourceMetadata: SourceMetadata?) {
        sourceMetadataMap[source] = sourceMetadata
    }

    // For backwards compatibility reason we extract the data from the deprecatedAnalyticsConfig
    // in case there is no source metadata
    fun getSourceMetadata(source: Any): SourceMetadata? {
        val sourceMetadata = sourceMetadataMap[source]
        val config = deprecatedBitmovinAnalyticsConfig

        if (sourceMetadata != null) {
            return sourceMetadata
        } else if (config != null) {
            return ApiV3Utils.extractSourceMetadata(config)
        } else {
            return null
        }
    }

    fun getSourceMetadata(): SourceMetadata? {
        return getSourceMetadata(DEFAULT_KEY)
    }

    fun setSourceMetadata(sourceMetadata: SourceMetadata?) {
        setSourceMetadata(DEFAULT_KEY, sourceMetadata)
    }

    // TODO: better naming, or check how i can use a backing field easier
    private var internalDefaultMetadata: DefaultMetadata? = null

    var defaultMetadata: DefaultMetadata
        get() {
            val explicitSetDefaultMetadata = internalDefaultMetadata
            return if (explicitSetDefaultMetadata != null) {
                explicitSetDefaultMetadata
            } else if (deprecatedBitmovinAnalyticsConfig != null) {
                ApiV3Utils.extractDefaultMetadata(deprecatedBitmovinAnalyticsConfig!!)
            } else {
                DefaultMetadata()
            }
        }
        set(value) {
            internalDefaultMetadata = value
        }

    companion object {
        private val DEFAULT_KEY = Any()
    }
}
