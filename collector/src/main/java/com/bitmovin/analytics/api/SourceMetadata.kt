package com.bitmovin.analytics.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// TODO: add field descriptions
@Parcelize
data class SourceMetadata
@JvmOverloads
constructor(
    val title: String? = null,
    val videoId: String? = null,
    val cdnProvider: String? = null,
    val mpdUrl: String? = null,
    val m3u8Url: String? = null,
    val progUrl: String? = null,
    val path: String? = null,
    val isLive: Boolean? = null,
    val customData: CustomData = CustomData(),
) : Parcelable
