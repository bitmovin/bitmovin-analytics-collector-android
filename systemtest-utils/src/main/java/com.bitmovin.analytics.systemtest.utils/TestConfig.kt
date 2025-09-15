package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData

object TestConfig {
    /** Account: 'bitmovin-analytics', Analytics License: 'Local Development License Key" */
    fun createBitmovinAnalyticsConfig(
        analyticsKey: String = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0",
        title: String = MetadataUtils.MetadataGenerator.getTestTitle(),
        backendUrl: String? = null,
    ): BitmovinAnalyticsConfig {
        val bitmovinAnalyticsConfig =
            BitmovinAnalyticsConfig(analyticsKey)
        bitmovinAnalyticsConfig.title = title
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
        bitmovinAnalyticsConfig.customData50 = "customData50"
        bitmovinAnalyticsConfig.path = "/customPath/new/"
        bitmovinAnalyticsConfig.cdnProvider = "testCdnProvider"
        if (backendUrl != null) {
            bitmovinAnalyticsConfig.config.backendUrl = backendUrl
        }
        return bitmovinAnalyticsConfig
    }

    fun createAnalyticsConfig(
        // Android Testing License Key
        analyticsKey: String = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0",
        backendUrl: String? = null,
        ssaiEngagementTrackingEnabled: Boolean = true,
    ): AnalyticsConfig {
        if (backendUrl == null) {
            return AnalyticsConfig(
                licenseKey = analyticsKey,
                logLevel = com.bitmovin.analytics.api.LogLevel.DEBUG,
                ssaiEngagementTrackingEnabled = ssaiEngagementTrackingEnabled,
            )
        }

        return AnalyticsConfig(
            licenseKey = analyticsKey,
            backendUrl = backendUrl,
            logLevel = com.bitmovin.analytics.api.LogLevel.DEBUG,
            ssaiEngagementTrackingEnabled = ssaiEngagementTrackingEnabled,
        )
    }

    fun createDummyCustomData(prefix: String = "customData"): CustomData {
        return CustomData(
            experimentName = "dummyExperiment",
            customData1 = prefix + "1", customData2 = prefix + "2", customData3 = prefix + "3",
            customData4 = prefix + "4", customData5 = prefix + "5", customData6 = prefix + "6",
            customData7 = prefix + "7", customData8 = prefix + "8", customData9 = prefix + "9",
            customData10 = prefix + "10", customData11 = prefix + "11", customData12 = prefix + "12",
            customData13 = prefix + "13", customData14 = prefix + "14", customData15 = prefix + "15",
            customData16 = prefix + "16", customData17 = prefix + "17", customData18 = prefix + "18",
            customData19 = prefix + "19", customData20 = prefix + "20", customData21 = prefix + "21",
            customData22 = prefix + "22", customData23 = prefix + "23", customData24 = prefix + "24",
            customData25 = prefix + "25", customData26 = prefix + "26", customData27 = prefix + "27",
            customData28 = prefix + "28", customData29 = prefix + "29", customData30 = prefix + "30",
            customData31 = prefix + "31", customData32 = prefix + "32", customData33 = prefix + "33",
            customData34 = prefix + "34", customData35 = prefix + "35", customData36 = prefix + "36",
            customData37 = prefix + "37", customData38 = prefix + "38", customData39 = prefix + "39",
            customData40 = prefix + "40", customData41 = prefix + "41", customData42 = prefix + "42",
            customData43 = prefix + "43", customData44 = prefix + "44", customData45 = prefix + "45",
            customData46 = prefix + "46", customData47 = prefix + "47", customData48 = prefix + "48",
            customData49 = prefix + "49", customData50 = prefix + "50",
        )
    }
}
