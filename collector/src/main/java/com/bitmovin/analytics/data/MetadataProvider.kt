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
        val deprecatedOldConfig = deprecatedBitmovinAnalyticsConfig.get()

        if (deprecatedOldConfig != null) {
            val oldSourceMetadata = ApiV3Utils.extractSourceMetadata(deprecatedOldConfig)
            return ApiV3Utils.mergeSourceMetadata(sourceMetadata ?: SourceMetadata(), oldSourceMetadata)
        }

        return sourceMetadata
    }

    fun sourceMetadataIsSet(): Boolean {
        return sourceMetadataMap[DEFAULT_KEY] != null
    }

    fun getSourceMetadata(): SourceMetadata? {
        return getSourceMetadata(DEFAULT_KEY)
    }

    fun setSourceMetadata(sourceMetadata: SourceMetadata?) {
        setSourceMetadata(DEFAULT_KEY, sourceMetadata)
    }

    // For backwards compatibility reason we extract the data from the deprecatedAnalyticsConfig
    // in case there is no defaultMetadata
    // if there is default metadata, it means that the new API is used (defaultMetadata can only be set
    // through v3 factory method, and not through v2)
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

    fun setDeprecatedBitmovinAnalyticsConfig(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) {
        deprecatedBitmovinAnalyticsConfig.set(bitmovinAnalyticsConfig)
    }

    fun deprecatedBitmovinAnalyticsConfigIsSet(): Boolean {
        return deprecatedBitmovinAnalyticsConfig.get() != null
    }

    fun getDeprecatedBitmovinAnalyticsConfig(): BitmovinAnalyticsConfig? {
        return deprecatedBitmovinAnalyticsConfig.get()
    }

    companion object {
        private val DEFAULT_KEY = Any()
    }
}
