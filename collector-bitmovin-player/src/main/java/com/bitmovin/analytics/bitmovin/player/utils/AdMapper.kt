package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.*
import com.bitmovin.player.model.advertising.LinearAd
import com.bitmovin.player.model.advertising.VastAdData

class AdMapper {

    fun FromPlayerAd(playerAd: com.bitmovin.player.model.advertising.Ad): Ad{
        return FromPlayerAd(Ad(), playerAd)
    }

    fun FromPlayerAd(collectorAd : Ad, playerAd: com.bitmovin.player.model.advertising.Ad): Ad{

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

        if (playerAd.data is VastAdData)
            FromVastAdData(collectorAd, playerAd.data as VastAdData)

        if (playerAd.data is com.bitmovin.player.model.advertising.ima.ImaAdData)
            collectorAd.dealId = (playerAd.data as com.bitmovin.player.model.advertising.ima.ImaAdData).dealId

        if (playerAd is LinearAd)
            FromLinearAd(collectorAd, playerAd)

        return collectorAd
    }

    fun FromLinearAd(collectorAd : Ad, linearAd: LinearAd): Ad {

        collectorAd.duration = linearAd.duration?.toLong()
        collectorAd.skippable = linearAd.skippable
        collectorAd.skippableAfter = linearAd.skippableAfter?.toLong()

        return collectorAd
    }

    fun FromVastAdData(collectorAd: Ad, vastData: VastAdData): Ad{

        collectorAd.title = vastData.adTitle
        collectorAd.adSystemName = vastData.adSystem?.name
        collectorAd.adSystemVersion = vastData.adSystem?.version
        collectorAd.wrapperAdsCount = vastData.wrapperAdIds.size
        collectorAd.description = vastData.adDescription
        collectorAd.advertiserId = vastData.advertiser?.id
        collectorAd.advertiserName = vastData.advertiser?.name
        collectorAd.apiFramework = vastData.apiFramework
        collectorAd.creativeAdId = vastData.creative?.adId
        collectorAd.creativeId = vastData.creative?.id
        collectorAd.universalAdIdRegistry = vastData.creative?.universalAdId?.idRegistry
        collectorAd.universalAdIdValue = vastData.creative?.universalAdId?.value
        collectorAd.mediaFileUrl = vastData.mediaFileId
        collectorAd.codec = vastData.codec
        collectorAd.minSuggestedDuration = vastData.minSuggestedDuration?.toLong()
        collectorAd.pricingCurrency = vastData.pricing?.currency
        collectorAd.pricingModel = vastData.pricing?.model
        collectorAd.pricingValue = vastData.pricing?.value?.toLong()
        collectorAd.surveyType = vastData.survey?.type
        collectorAd.surveyUrl = vastData.survey?.uri

        return collectorAd
    }
}