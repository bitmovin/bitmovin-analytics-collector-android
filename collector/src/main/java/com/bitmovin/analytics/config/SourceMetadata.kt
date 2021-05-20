package com.bitmovin.analytics.config

import com.bitmovin.analytics.data.CustomData

data class SourceMetadata(
    val title: String? = null,
    val videoId: String? = null,
    val cdnProvider: String? = null,
    var experimentName: String? = null,
    val mpdUrl: String? = null,
    val m3u8Url: String? = null,
    val path: String? = null,
    val isLive: Boolean? = null,
    var customData1: String? = null,
    var customData2: String? = null,
    var customData3: String? = null,
    var customData4: String? = null,
    var customData5: String? = null,
    var customData6: String? = null,
    var customData7: String? = null
) {
    fun setCustomData(customData: CustomData) {
        customData1 = customData.customData1
        customData2 = customData.customData2
        customData3 = customData.customData3
        customData4 = customData.customData4
        customData5 = customData.customData5
        customData6 = customData.customData6
        customData7 = customData.customData7
        experimentName = customData.experimentName
    }

    fun getCustomData(): CustomData {
        return CustomData(customData1, customData2, customData3, customData4, customData5, customData6, customData7, experimentName)
    }
}
