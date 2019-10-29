package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.*
import java.util.ArrayList

class AdBreakFactory {
    fun FromPlayerImaAdBreak(imaAdBreak: com.bitmovin.player.model.advertising.ima.ImaAdBreak): AdBreak {
        val ads = ArrayList<Ad>(imaAdBreak.ads.size)
        if (imaAdBreak.ads.isNotEmpty())
            imaAdBreak.ads.forEach { ad ->  ads.add(AdFactory().FromPlayerAd(ad))}
        return AdBreak(
                id = imaAdBreak.id,
                ads = ads,
                scheduleTime = imaAdBreak.scheduleTime.toLong(),
                replaceContentDuration = imaAdBreak.replaceContentDuration!!.toLong(),
                position = getPositionFromPlayerPosition(imaAdBreak.position),
                fallbackIndex = imaAdBreak.currentFallbackIndex?.toLong() ?: 0,
                tagType = AdTagFactory().FromPlayerAdTag(imaAdBreak.tag),
                tagUrl = imaAdBreak.tag.url
        )
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