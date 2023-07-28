package com.bitmovin.analytics.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * DefaultMetadata that can be used to enrich the analytics data.
 * DefaultMetadata is not bound to a specific source and can be used to set
 * fields for the lifecycle of the collector.
 * If fields are specified in SourceMetadata and DefaultMetadata, SourceMetadata takes precedence.
 */
@Parcelize
data class DefaultMetadata
@JvmOverloads
constructor(
    /**
     * CDN Provider used to serve content.
     * If field is specified in SourceMetadata and DefaultMetadata, SourceMetadata takes precedence.
     */
    val cdnProvider: String? = null,
    /**
     * Field that can be used to mark a session with the internal User-ID.
     */
    val customUserId: String? = null,
    /**
     * Free-form data that can be used to enrich the analytics data.
     * If customData is specified in SourceMetadata and DefaultMetadata
     * data is merged on a field basis with SourceMetadata taking precedence.
     */
    val customData: CustomData = CustomData(),
) : Parcelable {

    class Builder {
        private var cdnProvider: String? = null
        private var customUserId: String? = null
        private var customData: CustomData = CustomData()

        fun setCdnProvider(cdnProvider: String?) = apply { this.cdnProvider = cdnProvider }
        fun setCustomUserId(customUserId: String?) = apply { this.customUserId = customUserId }
        fun setCustomData(customData: CustomData) = apply { this.customData = customData }

        fun build(): DefaultMetadata {
            return DefaultMetadata(
                cdnProvider = cdnProvider,
                customUserId = customUserId,
                customData = customData,
            )
        }
    }
}
