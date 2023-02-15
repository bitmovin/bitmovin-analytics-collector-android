package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.Ad
import com.bitmovin.player.api.advertising.LinearAd
import com.bitmovin.player.api.advertising.ima.ImaAdData
import com.bitmovin.player.api.advertising.vast.VastAdData

internal class AdMapper {

    fun fromPlayerAd(playerAd: com.bitmovin.player.api.advertising.Ad): Ad {
        return fromPlayerAd(Ad(), playerAd)
    }

    fun fromPlayerAd(collectorAd: Ad, playerAd: com.bitmovin.player.api.advertising.Ad): Ad {
        collectorAd.isLinear = playerAd.isLinear
        collectorAd.width = playerAd.width
        collectorAd.height = playerAd.height
        collectorAd.id = playerAd.id
        collectorAd.mediaFileUrl = playerAd.mediaFileUrl
        collectorAd.clickThroughUrl = playerAd.clickThroughUrl
        collectorAd.bitrate = playerAd.data?.bitrate
        collectorAd.minBitrate = playerAd.data?.minBitrate
        collectorAd.maxBitrate = playerAd.data?.maxBitrate
        collectorAd.mimeType = playerAd.data?.mimeType

        if (playerAd.data is VastAdData) {
            fromVastAdData(collectorAd, playerAd.data as VastAdData)
        }

        if (playerAd.data is ImaAdData) {
            collectorAd.dealId = (playerAd.data as ImaAdData).dealId
        }

        if (playerAd is LinearAd) {
            fromLinearAd(collectorAd, playerAd)
        }

        return collectorAd
    }

    private fun fromLinearAd(collectorAd: Ad, linearAd: LinearAd) {
        collectorAd.duration = linearAd.duration?.toLong()?.times(1000)
        collectorAd.skippable = linearAd.skippableAfter != null
        collectorAd.skippableAfter = linearAd.skippableAfter?.toLong()?.times(1000)
    }

    private fun fromVastAdData(collectorAd: Ad, vastData: VastAdData) {
        collectorAd.title = vastData.adTitle
        collectorAd.adSystemName = vastData.adSystem?.name
        collectorAd.adSystemVersion = vastData.adSystem?.version
        collectorAd.wrapperAdsCount = vastData.wrapperAdIds?.size
        collectorAd.description = vastData.adDescription
        collectorAd.advertiserId = vastData.advertiser?.id
        collectorAd.advertiserName = vastData.advertiser?.name
        collectorAd.apiFramework = vastData.apiFramework
        collectorAd.creativeAdId = vastData.creative?.adId
        collectorAd.creativeId = vastData.creative?.id
        collectorAd.universalAdIdRegistry = vastData.creative?.universalAdId?.idRegistry
        collectorAd.universalAdIdValue = vastData.creative?.universalAdId?.value
        collectorAd.codec = vastData.codec
        collectorAd.minSuggestedDuration = vastData.minSuggestedDuration?.toLong()?.times(1000)
        collectorAd.pricingCurrency = vastData.pricing?.currency
        collectorAd.pricingModel = vastData.pricing?.model
        collectorAd.pricingValue = vastData.pricing?.value?.toLong()
        collectorAd.surveyType = vastData.survey?.type
        collectorAd.surveyUrl = vastData.survey?.uri
    }
}
