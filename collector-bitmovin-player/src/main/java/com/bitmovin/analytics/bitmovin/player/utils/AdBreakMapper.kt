package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.Ad
import com.bitmovin.analytics.ads.AdBreak
import com.bitmovin.analytics.ads.AdPosition
import com.bitmovin.analytics.bitmovin.player.BitmovinUtil
import com.bitmovin.analytics.utils.secondsToMillisecondsLong
import com.bitmovin.player.api.advertising.AdBreakConfig
import com.bitmovin.player.api.advertising.AdConfig
import com.bitmovin.player.api.advertising.ima.ImaAdBreak
import java.util.ArrayList

internal class AdBreakMapper {
    fun fromPlayerAdConfiguration(adConfig: AdConfig): AdBreak {
        val collectorAdBreak = AdBreak("notset", ArrayList<Ad>() as List<Ad>)

        fromPlayerAdConfiguration(collectorAdBreak, adConfig)

        return collectorAdBreak
    }

    private fun fromPlayerAdConfiguration(
        collectorAdBreak: AdBreak,
        adConfig: AdConfig,
    ): AdBreak {
        collectorAdBreak.replaceContentDuration = adConfig.replaceContentDuration?.secondsToMillisecondsLong()

        if (adConfig is com.bitmovin.player.api.advertising.AdBreak) {
            fromPlayerAdBreak(collectorAdBreak, adConfig)
        } else if (adConfig is com.bitmovin.player.api.advertising.AdBreakConfig) {
            // in cases of errors the adConfig is not inheriting form AdBreak anymore
            fromPlayerConfig(collectorAdBreak, adConfig)
        }

        return collectorAdBreak
    }

    private fun fromPlayerConfig(
        collectorAdBreak: AdBreak,
        playerAdBreakConfig: AdBreakConfig,
    ) {
        collectorAdBreak.id = playerAdBreakConfig.id
        collectorAdBreak.position = BitmovinUtil.getAdPositionFromPlayerPosition(playerAdBreakConfig.position)
        collectorAdBreak.tagType = AdTagFactory.fromPlayerAdTag(playerAdBreakConfig.tag)
        collectorAdBreak.tagUrl = playerAdBreakConfig.tag.url
    }

    private fun fromPlayerAdBreak(
        collectorAdBreak: AdBreak,
        playerAdBreak: com.bitmovin.player.api.advertising.AdBreak,
    ) {
        val ads = ArrayList<Ad>(playerAdBreak.ads.size)
        if (playerAdBreak.ads.isNotEmpty()) {
            playerAdBreak.ads.forEach { ad -> ads.add(AdMapper().fromPlayerAd(Ad(), ad)) }
        }

        collectorAdBreak.id = playerAdBreak.id
        collectorAdBreak.ads = ads

        collectorAdBreak.scheduleTime = playerAdBreak.scheduleTime.secondsToMillisecondsLong()

        if (playerAdBreak is ImaAdBreak) {
            fromImaAdBreak(collectorAdBreak, playerAdBreak)
        } else {
            fromDefaultAdBreak(collectorAdBreak, playerAdBreak)
        }
    }

    private fun fromImaAdBreak(
        collectorAdBreak: AdBreak,
        imaAdBreak: ImaAdBreak,
    ) {
        collectorAdBreak.position = BitmovinUtil.getAdPositionFromPlayerPosition(imaAdBreak.position)
        collectorAdBreak.fallbackIndex = imaAdBreak.currentFallbackIndex?.toLong() ?: 0
        collectorAdBreak.tagType = AdTagFactory.fromPlayerAdTag(imaAdBreak.tag)
        collectorAdBreak.tagUrl = imaAdBreak.tag.url
    }

    private fun fromDefaultAdBreak(
        collectorAdBreak: AdBreak,
        adBreak: com.bitmovin.player.api.advertising.AdBreak,
    ) {
        collectorAdBreak.position = getPositionFromScheduledTime(adBreak.scheduleTime)
        collectorAdBreak.replaceContentDuration = adBreak.replaceContentDuration?.secondsToMillisecondsLong()
    }

    private fun getPositionFromScheduledTime(scheduleTime: Double): AdPosition? {
        // we only detect pre roll for now since
        // we would need to compare the asset duration with the schedule time
        // for mid or post roll detection, which might get inaccurate
        if (scheduleTime == 0.0) {
            return AdPosition.PRE
        }

        return null
    }
}
