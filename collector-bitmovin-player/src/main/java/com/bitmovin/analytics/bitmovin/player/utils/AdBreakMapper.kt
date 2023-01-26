package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.Ad
import com.bitmovin.analytics.ads.AdBreak
import com.bitmovin.analytics.ads.AdPosition
import com.bitmovin.player.api.advertising.AdConfig
import com.bitmovin.player.api.advertising.ima.ImaAdBreak
import java.util.ArrayList

class AdBreakMapper {

    fun fromPlayerAdConfiguration(adConfig: AdConfig): AdBreak {
        val collectorAdBreak = AdBreak("notset", ArrayList<Ad>() as List<Ad>)

        fromPlayerAdConfiguration(collectorAdBreak, adConfig)

        return collectorAdBreak
    }

    private fun fromPlayerAdConfiguration(collectorAdBreak: AdBreak, adConfig: AdConfig): AdBreak {
        collectorAdBreak.replaceContentDuration = adConfig.replaceContentDuration?.toLong()?.times(1000)

        if (adConfig is com.bitmovin.player.api.advertising.AdBreak) {
            fromPlayerAdBreak(collectorAdBreak, adConfig)
        }

        return collectorAdBreak
    }

    private fun fromPlayerAdBreak(collectorAdBreak: AdBreak, playerAdBreak: com.bitmovin.player.api.advertising.AdBreak) {
        val ads = ArrayList<Ad>(playerAdBreak.ads.size)
        if (playerAdBreak.ads.isNotEmpty()) {
            playerAdBreak.ads.forEach { ad -> ads.add(AdMapper().fromPlayerAd(Ad(), ad)) }
        }

        collectorAdBreak.id = playerAdBreak.id
        collectorAdBreak.ads = ads

        collectorAdBreak.scheduleTime = playerAdBreak.scheduleTime.toLong().times(1000)

        if (playerAdBreak is ImaAdBreak) {
            fromImaAdBreak(collectorAdBreak, playerAdBreak)
        }
    }

    private fun fromImaAdBreak(collectorAdBreak: AdBreak, imaAdBreak: ImaAdBreak) {
        collectorAdBreak.position = getPositionFromPlayerPosition(imaAdBreak.position)
        collectorAdBreak.fallbackIndex = imaAdBreak.currentFallbackIndex?.toLong() ?: 0
        collectorAdBreak.tagType = AdTagFactory().FromPlayerAdTag(imaAdBreak.tag)
        collectorAdBreak.tagUrl = imaAdBreak.tag.url
    }

    private fun getPositionFromPlayerPosition(playerPosition: String): AdPosition? {
        return when {
            playerPosition == "pre" -> AdPosition.pre
            playerPosition == "post" -> AdPosition.post
            "([0-9]+.*)".toRegex().matches(playerPosition) -> AdPosition.mid
            else -> null
        }
    }
}
