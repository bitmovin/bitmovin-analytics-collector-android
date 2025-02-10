package com.bitmovin.analytics.utils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.RetryPolicy
import com.bitmovin.analytics.api.SourceMetadata

object ApiV3Utils {
    fun extractAnalyticsConfig(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig): AnalyticsConfig {
        return AnalyticsConfig(
            licenseKey = bitmovinAnalyticsConfig.key,
            adTrackingDisabled = !bitmovinAnalyticsConfig.ads,
            randomizeUserId = bitmovinAnalyticsConfig.randomizeUserId,
            backendUrl = bitmovinAnalyticsConfig.config.backendUrl,
            retryPolicy = bitmovinAnalyticsConfig.retryPolicy,
        )
    }

    fun extractDefaultMetadata(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig): DefaultMetadata {
        val customData =
            CustomData(
                customData1 = bitmovinAnalyticsConfig.customData1,
                customData2 = bitmovinAnalyticsConfig.customData2,
                customData3 = bitmovinAnalyticsConfig.customData3,
                customData4 = bitmovinAnalyticsConfig.customData4,
                customData5 = bitmovinAnalyticsConfig.customData5,
                customData6 = bitmovinAnalyticsConfig.customData6,
                customData7 = bitmovinAnalyticsConfig.customData7,
                customData8 = bitmovinAnalyticsConfig.customData8,
                customData9 = bitmovinAnalyticsConfig.customData9,
                customData10 = bitmovinAnalyticsConfig.customData10,
                customData11 = bitmovinAnalyticsConfig.customData11,
                customData12 = bitmovinAnalyticsConfig.customData12,
                customData13 = bitmovinAnalyticsConfig.customData13,
                customData14 = bitmovinAnalyticsConfig.customData14,
                customData15 = bitmovinAnalyticsConfig.customData15,
                customData16 = bitmovinAnalyticsConfig.customData16,
                customData17 = bitmovinAnalyticsConfig.customData17,
                customData18 = bitmovinAnalyticsConfig.customData18,
                customData19 = bitmovinAnalyticsConfig.customData19,
                customData20 = bitmovinAnalyticsConfig.customData20,
                customData21 = bitmovinAnalyticsConfig.customData21,
                customData22 = bitmovinAnalyticsConfig.customData22,
                customData23 = bitmovinAnalyticsConfig.customData23,
                customData24 = bitmovinAnalyticsConfig.customData24,
                customData25 = bitmovinAnalyticsConfig.customData25,
                customData26 = bitmovinAnalyticsConfig.customData26,
                customData27 = bitmovinAnalyticsConfig.customData27,
                customData28 = bitmovinAnalyticsConfig.customData28,
                customData29 = bitmovinAnalyticsConfig.customData29,
                customData30 = bitmovinAnalyticsConfig.customData30,
                experimentName = bitmovinAnalyticsConfig.experimentName,
                customData31 = bitmovinAnalyticsConfig.customData31,
                customData32 = bitmovinAnalyticsConfig.customData32,
                customData33 = bitmovinAnalyticsConfig.customData33,
                customData34 = bitmovinAnalyticsConfig.customData34,
                customData35 = bitmovinAnalyticsConfig.customData35,
                customData36 = bitmovinAnalyticsConfig.customData36,
                customData37 = bitmovinAnalyticsConfig.customData37,
                customData38 = bitmovinAnalyticsConfig.customData38,
                customData39 = bitmovinAnalyticsConfig.customData39,
                customData40 = bitmovinAnalyticsConfig.customData40,
                customData41 = bitmovinAnalyticsConfig.customData41,
                customData42 = bitmovinAnalyticsConfig.customData42,
                customData43 = bitmovinAnalyticsConfig.customData43,
                customData44 = bitmovinAnalyticsConfig.customData44,
                customData45 = bitmovinAnalyticsConfig.customData45,
                customData46 = bitmovinAnalyticsConfig.customData46,
                customData47 = bitmovinAnalyticsConfig.customData47,
                customData48 = bitmovinAnalyticsConfig.customData48,
                customData49 = bitmovinAnalyticsConfig.customData49,
                customData50 = bitmovinAnalyticsConfig.customData50,
            )

        return DefaultMetadata(
            cdnProvider = bitmovinAnalyticsConfig.cdnProvider,
            customUserId = bitmovinAnalyticsConfig.customUserId,
            customData = customData,
        )
    }

    fun extractSourceMetadata(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig): SourceMetadata {
        return SourceMetadata(
            title = bitmovinAnalyticsConfig.title,
            videoId = bitmovinAnalyticsConfig.videoId,
            cdnProvider = bitmovinAnalyticsConfig.cdnProvider,
            path = bitmovinAnalyticsConfig.path,
            isLive = bitmovinAnalyticsConfig.isLive,
            // customData is not extracted here, since we map it to
            // defaultMetadata to mimic the API v2 behaviour
        )
    }

    fun mergeSourceMetadata(
        sourceMetadata: SourceMetadata,
        fallBack: SourceMetadata,
    ): SourceMetadata {
        return SourceMetadata(
            title = sourceMetadata.title ?: fallBack.title,
            videoId = sourceMetadata.videoId ?: fallBack.videoId,
            cdnProvider = sourceMetadata.cdnProvider ?: fallBack.cdnProvider,
            path = sourceMetadata.path ?: fallBack.path,
            isLive = sourceMetadata.isLive ?: fallBack.isLive,
            customData = mergeCustomData(sourceMetadata.customData, fallBack.customData),
        )
    }

    fun mergeSourceMetadata(
        sourceMetadata: SourceMetadata?,
        defaultMetadata: DefaultMetadata?,
    ): SourceMetadata {
        val mergedCustomData =
            mergeCustomData(sourceMetadata?.customData, defaultMetadata?.customData)

        if (sourceMetadata == null) {
            return SourceMetadata(
                cdnProvider = defaultMetadata?.cdnProvider,
                customData = mergedCustomData,
            )
        }

        return sourceMetadata.copy(customData = mergedCustomData, cdnProvider = sourceMetadata.cdnProvider ?: defaultMetadata?.cdnProvider)
    }

    fun mergeCustomData(
        mainCustomData: CustomData?,
        fallbackCustomData: CustomData?,
    ): CustomData {
        return CustomData(
            customData1 = mainCustomData?.customData1 ?: fallbackCustomData?.customData1,
            customData2 = mainCustomData?.customData2 ?: fallbackCustomData?.customData2,
            customData3 = mainCustomData?.customData3 ?: fallbackCustomData?.customData3,
            customData4 = mainCustomData?.customData4 ?: fallbackCustomData?.customData4,
            customData5 = mainCustomData?.customData5 ?: fallbackCustomData?.customData5,
            customData6 = mainCustomData?.customData6 ?: fallbackCustomData?.customData6,
            customData7 = mainCustomData?.customData7 ?: fallbackCustomData?.customData7,
            customData8 = mainCustomData?.customData8 ?: fallbackCustomData?.customData8,
            customData9 = mainCustomData?.customData9 ?: fallbackCustomData?.customData9,
            customData10 = mainCustomData?.customData10 ?: fallbackCustomData?.customData10,
            customData11 = mainCustomData?.customData11 ?: fallbackCustomData?.customData11,
            customData12 = mainCustomData?.customData12 ?: fallbackCustomData?.customData12,
            customData13 = mainCustomData?.customData13 ?: fallbackCustomData?.customData13,
            customData14 = mainCustomData?.customData14 ?: fallbackCustomData?.customData14,
            customData15 = mainCustomData?.customData15 ?: fallbackCustomData?.customData15,
            customData16 = mainCustomData?.customData16 ?: fallbackCustomData?.customData16,
            customData17 = mainCustomData?.customData17 ?: fallbackCustomData?.customData17,
            customData18 = mainCustomData?.customData18 ?: fallbackCustomData?.customData18,
            customData19 = mainCustomData?.customData19 ?: fallbackCustomData?.customData19,
            customData20 = mainCustomData?.customData20 ?: fallbackCustomData?.customData20,
            customData21 = mainCustomData?.customData21 ?: fallbackCustomData?.customData21,
            customData22 = mainCustomData?.customData22 ?: fallbackCustomData?.customData22,
            customData23 = mainCustomData?.customData23 ?: fallbackCustomData?.customData23,
            customData24 = mainCustomData?.customData24 ?: fallbackCustomData?.customData24,
            customData25 = mainCustomData?.customData25 ?: fallbackCustomData?.customData25,
            customData26 = mainCustomData?.customData26 ?: fallbackCustomData?.customData26,
            customData27 = mainCustomData?.customData27 ?: fallbackCustomData?.customData27,
            customData28 = mainCustomData?.customData28 ?: fallbackCustomData?.customData28,
            customData29 = mainCustomData?.customData29 ?: fallbackCustomData?.customData29,
            customData30 = mainCustomData?.customData30 ?: fallbackCustomData?.customData30,
            experimentName = mainCustomData?.experimentName ?: fallbackCustomData?.experimentName,
            customData31 = mainCustomData?.customData31 ?: fallbackCustomData?.customData31,
            customData32 = mainCustomData?.customData32 ?: fallbackCustomData?.customData32,
            customData33 = mainCustomData?.customData33 ?: fallbackCustomData?.customData33,
            customData34 = mainCustomData?.customData34 ?: fallbackCustomData?.customData34,
            customData35 = mainCustomData?.customData35 ?: fallbackCustomData?.customData35,
            customData36 = mainCustomData?.customData36 ?: fallbackCustomData?.customData36,
            customData37 = mainCustomData?.customData37 ?: fallbackCustomData?.customData37,
            customData38 = mainCustomData?.customData38 ?: fallbackCustomData?.customData38,
            customData39 = mainCustomData?.customData39 ?: fallbackCustomData?.customData39,
            customData40 = mainCustomData?.customData40 ?: fallbackCustomData?.customData40,
            customData41 = mainCustomData?.customData41 ?: fallbackCustomData?.customData41,
            customData42 = mainCustomData?.customData42 ?: fallbackCustomData?.customData42,
            customData43 = mainCustomData?.customData43 ?: fallbackCustomData?.customData43,
            customData44 = mainCustomData?.customData44 ?: fallbackCustomData?.customData44,
            customData45 = mainCustomData?.customData45 ?: fallbackCustomData?.customData45,
            customData46 = mainCustomData?.customData46 ?: fallbackCustomData?.customData46,
            customData47 = mainCustomData?.customData47 ?: fallbackCustomData?.customData47,
            customData48 = mainCustomData?.customData48 ?: fallbackCustomData?.customData48,
            customData49 = mainCustomData?.customData49 ?: fallbackCustomData?.customData49,
            customData50 = mainCustomData?.customData50 ?: fallbackCustomData?.customData50,
        )
    }

    private val BitmovinAnalyticsConfig.retryPolicy get() =
        when {
            config.longTermRetryEnabled -> RetryPolicy.LONG_TERM
            config.tryResendDataOnFailedConnection -> RetryPolicy.SHORT_TERM
            else -> RetryPolicy.NO_RETRY
        }
}
