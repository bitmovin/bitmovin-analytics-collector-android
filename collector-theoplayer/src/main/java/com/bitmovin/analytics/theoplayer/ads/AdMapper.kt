package com.bitmovin.analytics.theoplayer.ads

import com.bitmovin.analytics.ads.Ad
import com.bitmovin.analytics.theoplayer.TheoPlayerUtils
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.ads.LinearAd
import com.theoplayer.android.api.ads.ima.GoogleImaAd
import com.theoplayer.android.api.ads.Ad as TheoAd

internal object AdMapper {
    fun fromTheoAd(theoAd: TheoAd): Ad {
        val ad = Ad()
        ad.id = theoAd.id
        ad.isLinear = theoAd.type == "linear"
        val skipOffset = theoAd.skipOffset
        ad.skippable = skipOffset >= 0
        if (skipOffset >= 0) {
            ad.skippableAfter = skipOffset * 1000L
        }
        ad.adModule = theoAd.integration.type

        try {
            if (TheoPlayerUtils.isTheoImaClassLoaded && theoAd is GoogleImaAd) {
                extractMetadataFromGoogleImaAd(ad, theoAd)
                return ad
            }
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "on fromTheoAd", e)
        }

        if (theoAd is LinearAd) {
            extractMetadataFromLinearAd(ad, theoAd)
        }
        return ad
    }

    private fun extractMetadataFromLinearAd(
        ad: Ad,
        linearAd: LinearAd,
    ) {
        ad.duration = Util.secondsToMillis(linearAd.durationAsDouble)
    }

    private fun extractMetadataFromGoogleImaAd(
        ad: Ad,
        googleImaAd: GoogleImaAd,
    ) {
        ad.creativeId = googleImaAd.creativeId
        ad.creativeAdId = googleImaAd.imaAd.creativeAdId
        ad.adSystemName = googleImaAd.adSystem
        ad.title = extractImaTitleWithFallback(googleImaAd)
        ad.description = googleImaAd.imaAd.description
        ad.wrapperAdsCount = googleImaAd.wrapperAdIds.size
        ad.advertiserName = googleImaAd.imaAd.advertiserName
        ad.dealId = googleImaAd.imaAd.dealId
        ad.surveyUrl = googleImaAd.imaAd.surveyUrl
        ad.mimeType = googleImaAd.imaAd.contentType

        val firstUniversalAdId = googleImaAd.universalAdIds.firstOrNull()
        if (firstUniversalAdId != null) {
            ad.universalAdIdValue = firstUniversalAdId.universalAdIdValue
            ad.universalAdIdRegistry = firstUniversalAdId.universalAdIdRegistry
        }

        // measurements
        ad.duration = Util.secondsToMillis(googleImaAd.imaAd.duration)
        ad.bitrate = googleImaAd.vastMediaBitrate
        ad.width = googleImaAd.imaAd.width
        ad.height = googleImaAd.imaAd.height
    }

    private fun extractImaTitleWithFallback(googleImaAd: GoogleImaAd): String? {
        // we are defensive here, since doc says it can be null
        // might be version dependent
        val title: String? = googleImaAd.imaAd.title
        if (title != null && title.isNotEmpty()) {
            return title
        }

        // we are defensive here too
        val adId: String? = googleImaAd.id
        if (adId != null && adId.isNotEmpty()) {
            return adId
        }

        return null
    }

    private const val TAG = "AdMapper"
}
