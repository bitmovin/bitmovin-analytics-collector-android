package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.*
import com.bitmovin.player.model.advertising.AdConfiguration
import com.bitmovin.player.model.advertising.ima.ImaAdBreak
import java.util.ArrayList

class AdBreakMapper {

    fun FromPlayerAdConfiguration(adConfiguration: AdConfiguration): AdBreak{
        var collectorAdBreak = AdBreak("notset", ArrayList<Ad>() as List<Ad>)

        FromPlayerAdConfiguration(collectorAdBreak, adConfiguration)

        return collectorAdBreak
    }

    fun FromPlayerAdConfiguration(collectorAdBreak: AdBreak, adConfiguration: AdConfiguration): AdBreak{

        collectorAdBreak.replaceContentDuration = adConfiguration.replaceContentDuration?.toLong()?.times(1000)

        if (adConfiguration is com.bitmovin.player.model.advertising.AdBreak)
            FromPlayerAdBreak(collectorAdBreak, adConfiguration)

        return collectorAdBreak
    }


    private fun FromPlayerAdBreak(collectorAdBreak: AdBreak, playerAdBreak: com.bitmovin.player.model.advertising.AdBreak) {

        val ads = ArrayList<Ad>(playerAdBreak.ads.size)
        if (playerAdBreak.ads.isNotEmpty())
            playerAdBreak.ads.forEach { ad ->  ads.add(AdMapper().FromPlayerAd(Ad(),ad))}

        collectorAdBreak.id = playerAdBreak.id
        collectorAdBreak.ads = ads

        collectorAdBreak.scheduleTime = playerAdBreak.scheduleTime.toLong().times(1000)

        if (playerAdBreak is ImaAdBreak)
            FromImaAdBreak(collectorAdBreak, playerAdBreak)
    }

    private fun FromImaAdBreak(collectorAdBreak: AdBreak, imaAdBreak: ImaAdBreak){

        collectorAdBreak.position = getPositionFromPlayerPosition(imaAdBreak.position)
        collectorAdBreak.fallbackIndex = imaAdBreak.currentFallbackIndex?.toLong() ?: 0
        collectorAdBreak.tagType = AdTagFactory().FromPlayerAdTag(imaAdBreak.tag)
        collectorAdBreak.tagUrl = imaAdBreak.tag.url
    }


    private fun getPositionFromPlayerPosition(playerPosition: String): AdPosition?{
        return when {
            playerPosition == "pre" -> AdPosition.pre
            playerPosition == "post" -> AdPosition.post
            "([0-9]+.*)".toRegex().matches(playerPosition) -> AdPosition.mid
            else -> null
        }
    }
}