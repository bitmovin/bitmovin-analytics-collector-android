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
     * Human readable title of the video asset currently playing
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
     * Breadcrumb path
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
) : Parcelable
