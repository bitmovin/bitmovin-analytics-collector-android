package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.*
import com.bitmovin.player.model.advertising.AdConfiguration
import com.bitmovin.player.model.advertising.ima.ImaAdBreak
import java.util.ArrayList

class AdBreakMapper {

    fun FromPlayerAdConfiguration(adConfiguration: AdConfiguration): AdBreak{
        return FromPlayerAdBreak(adConfiguration as com.bitmovin.player.model.advertising.AdBreak)
    }

    fun FromPlayerAdBreak(playerAdBreak: com.bitmovin.player.model.advertising.AdBreak) : AdBreak {

        val ads = ArrayList<Ad>(playerAdBreak.ads.size)
        if (playerAdBreak.ads.isNotEmpty())
            playerAdBreak.ads.forEach { ad ->  ads.add(AdMapper().FromPlayerAd(Ad(),ad))}

        var collectorAdBreak = AdBreak(
                ads = ads,
                id = playerAdBreak.id
        )
        return FromPlayerAdBreak(collectorAdBreak, playerAdBreak)
    }

    fun FromPlayerAdBreak(collectorAdBreak: AdBreak, playerAdBreak: com.bitmovin.player.model.advertising.AdBreak) : AdBreak {

        collectorAdBreak.scheduleTime = playerAdBreak.scheduleTime.toLong()
        collectorAdBreak.replaceContentDuration = playerAdBreak.replaceContentDuration?.toLong()

        if (playerAdBreak is ImaAdBreak){
            collectorAdBreak.position = getPositionFromPlayerPosition(playerAdBreak.position)
            collectorAdBreak.fallbackIndex = playerAdBreak.currentFallbackIndex?.toLong() ?: 0
            collectorAdBreak.tagType = AdTagFactory().FromPlayerAdTag(playerAdBreak.tag)
            collectorAdBreak.tagUrl = playerAdBreak.tag.url
        }
        return collectorAdBreak

    }


    private fun getPositionFromPlayerPosition(playerPosition: String): AdPosition?{
        if (playerPosition == null)
            return null
        return when {
            playerPosition == "pre" -> AdPosition.pre
            playerPosition == "post" -> AdPosition.post
            playerPosition.contains("%") -> AdPosition.mid
            else -> null
        }
    }
}