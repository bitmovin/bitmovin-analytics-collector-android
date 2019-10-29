package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.*

class AdFactory {

    fun FromPlayerAd(playerAd: com.bitmovin.player.model.advertising.Ad): Ad{
        return Ad(
                isLinear = playerAd.isLinear,
                width = playerAd.width,
                height = playerAd.height,
                id = playerAd.id,
                mediaFileUrl = playerAd.mediaFileUrl,
                clickThroughUrl = playerAd.clickThroughUrl,
                bitrate = playerAd.data?.bitrate,
                minBitrate = playerAd.data?.minBitrate,
                maxBitrate = playerAd.data?.maxBitrate,
                mimeType = playerAd.data?.mimeType
        )
    }
}