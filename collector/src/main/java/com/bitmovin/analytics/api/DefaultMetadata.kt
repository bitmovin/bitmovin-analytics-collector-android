package com.bitmovin.analytics.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// TODO: add field descriptions
@Parcelize
data class DefaultMetadata
@JvmOverloads
constructor(
    val cdnProvider: String? = null,
    val customUserId: String? = null,
    val customData: CustomData = CustomData(),
) : Parcelable
