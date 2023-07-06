package com.bitmovin.analytics.data

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.utils.ApiV3Utils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class MetadataProvider {
    private val sourceMetadataMap = ConcurrentHashMap<Any, SourceMetadata?>()
    private val internalDefaultMetadata: AtomicReference<DefaultMetadata?> = AtomicReference(null)
    private val deprecatedBitmovinAnalyticsConfig: AtomicReference<BitmovinAnalyticsConfig?> = AtomicReference(null)

    fun setSourceMetadata(source: Any, sourceMetadata: SourceMetadata?) {
        sourceMetadataMap[source] = sourceMetadata
    }

    // For backwards compatibility reason we extract the data from the deprecatedAnalyticsConfig
    // in case there is no source metadata
    fun getSourceMetadata(source: Any): SourceMetadata? {
        val sourceMetadata = sourceMetadataMap[source]
        if (sourceMetadata != null) {
            return sourceMetadata
        }

        val config = deprecatedBitmovinAnalyticsConfig.get()
        if (config != null) {
            return ApiV3Utils.extractSourceMetadata(config)
        }

        return null
    }

    fun getSourceMetadata(): SourceMetadata? {
        return getSourceMetadata(DEFAULT_KEY)
    }

    fun setSourceMetadata(sourceMetadata: SourceMetadata?) {
        setSourceMetadata(DEFAULT_KEY, sourceMetadata)
    }

    // For backwards compatibility reason we extract the data from the deprecatedAnalyticsConfig
    // in case there is no defaultMetadata
    var defaultMetadata: DefaultMetadata
        get() {
            val explicitSetDefaultMetadata = internalDefaultMetadata.get()
            if (explicitSetDefaultMetadata != null) {
                return explicitSetDefaultMetadata
            }

            val config = deprecatedBitmovinAnalyticsConfig.get()
            if (config != null) {
                return ApiV3Utils.extractDefaultMetadata(config)
            }

            return DefaultMetadata()
        }
        set(value) {
            internalDefaultMetadata.set(value)
        }

    fun setDeprectedBitmovinAnalyticsConfig(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) {
        deprecatedBitmovinAnalyticsConfig.set(bitmovinAnalyticsConfig)
    }

    companion object {
        private val DEFAULT_KEY = Any()
    }
}
