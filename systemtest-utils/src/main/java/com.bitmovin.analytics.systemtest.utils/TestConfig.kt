package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.BitmovinAnalyticsConfig

object TestConfig {

    fun createBitmovinAnalyticsConfig(m3u8Url: String): BitmovinAnalyticsConfig {
        /** Account: 'bitmovin-analytics', Analytics License: 'Local Development License Key" */
        val bitmovinAnalyticsConfig =
            BitmovinAnalyticsConfig("17e6ea02-cb5a-407f-9d6b-9400358fbcc0")

        bitmovinAnalyticsConfig.title = "dummy title"
        bitmovinAnalyticsConfig.videoId = "dummy-videoId"
        bitmovinAnalyticsConfig.customUserId = "customBitmovinUserId1"
        bitmovinAnalyticsConfig.experimentName = "experiment-1"
        bitmovinAnalyticsConfig.customData1 = "systemtest"
        bitmovinAnalyticsConfig.customData2 = "customData2"
        bitmovinAnalyticsConfig.customData3 = "customData3"
        bitmovinAnalyticsConfig.customData4 = "customData4"
        bitmovinAnalyticsConfig.customData5 = "customData5"
        bitmovinAnalyticsConfig.customData6 = "customData6"
        bitmovinAnalyticsConfig.customData7 = "customData7"
        bitmovinAnalyticsConfig.path = "/customPath/new/"
        bitmovinAnalyticsConfig.m3u8Url = m3u8Url
        bitmovinAnalyticsConfig.cdnProvider = "testCdnProvider"
        return bitmovinAnalyticsConfig
    }
}
