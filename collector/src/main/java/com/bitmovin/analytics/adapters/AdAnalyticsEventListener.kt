package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.ads.Ad
import com.bitmovin.analytics.ads.AdBreak
import com.bitmovin.analytics.ads.AdQuartile

interface AdAnalyticsEventListener {
    fun onAdStarted(ad: Ad)
    fun onAdFinished()
    fun onAdBreakStarted(adBreak: AdBreak)
    fun onAdBreakFinished()
    fun onAdClicked(url: String?)
    fun onAdError(adBreak: AdBreak, code: Int?, message: String?)
    fun onAdSkipped()
    fun onAdManifestLoaded(adBreak: AdBreak, downloadTime: Long)
    fun onAdQuartile(quartile: AdQuartile)
    fun onPlay()
    fun onPause()
}
