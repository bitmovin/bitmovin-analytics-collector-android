package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.*
import java.util.ArrayList

class AdBreakFactory {
    fun FromPlayerAdBreak(playerAdBreak: com.bitmovin.player.model.advertising.AdBreak): AdBreak {
        val ads = ArrayList<Ad>(playerAdBreak.ads.size)
        if (playerAdBreak.ads.isNotEmpty())
            playerAdBreak.ads.forEach { ad ->  ads.add(AdFactory().FromPlayerAd(ad))}
        return AdBreak(
                id = playerAdBreak.id,
                scheduleTime = playerAdBreak.scheduleTime as Long,
                ads = ads
        )
    }
}