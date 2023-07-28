package com.bitmovin.analytics.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * SourceMetadata that can be used to enrich the analytics data.
 */
@Parcelize
data class SourceMetadata
@JvmOverloads
constructor(
    /**
     * Human readable title of the source.
     */
    val title: String? = null,
    /**
     * ID of the Video
     */
    val videoId: String? = null,
    /**
     * CDN Provider used to serve content.
     * If field is specified in SourceMetadata and DefaultMetadata, SourceMetadata takes precedence.
     */
    val cdnProvider: String? = null,
    /**
     * Breadcrumb within the app. For example, the name of the current activity.
     */
    val path: String? = null,
    /**
     * Mark the stream as live before stream metadata is available.
     * As soon as metadata is available, information from the player is used.
     */
    val isLive: Boolean? = null,
    /**
     * Free-form data that can be used to enrich the analytics data
     * If customData is specified in SourceMetadata and DefaultMetadata
     * data is merged on a field basis with SourceMetadata taking precedence.
     */
    val customData: CustomData = CustomData(),
) : Parcelable {

    class Builder {
        private var title: String? = null
        private var videoId: String? = null
        private var cdnProvider: String? = null
        private var path: String? = null
        private var isLive: Boolean? = null
        private var customData: CustomData = CustomData()

        fun setTitle(title: String?) = apply { this.title = title }
        fun setVideoId(videoId: String?) = apply { this.videoId = videoId }
        fun setCdnProvider(cdnProvider: String?) = apply { this.cdnProvider = cdnProvider }
        fun setPath(path: String?) = apply { this.path = path }
        fun setIsLive(isLive: Boolean?) = apply { this.isLive = isLive }
        fun setCustomData(customData: CustomData) = apply { this.customData = customData }

        fun build(): SourceMetadata {
            return SourceMetadata(
                title = title,
                videoId = videoId,
                cdnProvider = cdnProvider,
                path = path,
                isLive = isLive,
                customData = customData,
            )
        }
    }
}
