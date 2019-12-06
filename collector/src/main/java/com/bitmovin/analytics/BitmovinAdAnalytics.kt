package com.bitmovin.analytics

import com.bitmovin.analytics.ads.*
import com.bitmovin.analytics.data.AdSample
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.utils.Util

class BitmovinAdAnalytics(var analytics: BitmovinAnalytics) {
    private var activeAdBreak: AdBreak? = null
    private var activeAdSample: AdSample? = null
    private var adPodPosition: Int = 0
    private var adStartupTimestamp: Long? = null
    private var beginPlayingTimestamp: Long? = null
    private var isPlaying: Boolean = false
    private val adManifestDownloadTimes: HashMap<String, Long> = hashMapOf()

    private var currentTime: Long? = null
        get() = if(this.isPlaying) {
            if(field == null || this.beginPlayingTimestamp == null) {
                null
            } else {
                field!! + Util.getElapsedTimestamp() - this.beginPlayingTimestamp!!
            }

        } else {
            field
        }

    fun onAdStarted(ad: Ad) {
        if (!ad.isLinear) {
            return
        }

        this.resetActiveAd()
        val adSample = AdSample(ad = ad)
        val adStartupTimestamp = this.adStartupTimestamp
        adSample.adStartupTime = if (adStartupTimestamp != null) Util.getElapsedTimestamp() - adStartupTimestamp else null

        this.activeAdSample = adSample

        this.startAd(adSample)
    }

    fun onAdFinished() {
        val activeAdSample = this.activeAdSample ?: return
        val activeAdBreak = this.activeAdBreak ?: return

        val adSample = activeAdSample.copy()
        adSample.completed = 1
        this.resetActiveAd()
        this.completeAd(activeAdBreak, adSample, adSample.ad.duration)
    }

    fun onAdBreakStarted(adBreak: AdBreak) {
        this.adPodPosition = 0
        this.activeAdBreak = adBreak
        this.adStartupTimestamp = Util.getElapsedTimestamp()
    }

    fun onAdBreakFinished() {
        this.resetActiveAd()
        this.activeAdBreak = null
    }

    fun onAdClicked(clickThroughUrl: String?) {
        val activeAdSample = this.activeAdSample ?: return

        activeAdSample.ad.clickThroughUrl = clickThroughUrl
        activeAdSample.clicked = 1
        activeAdSample.clickPosition = this.currentTime
        activeAdSample.clickPercentage = Util.calculatePercentage(activeAdSample.clickPosition, activeAdSample.ad.duration, true)
    }

    fun onAdError(adBreak: AdBreak, code: Int?, message: String?) {
        val adSample = this.activeAdSample ?: AdSample()

        if (adSample.ad.id != null && adBreak.ads.any { ad -> ad.id == adSample.ad.id }) {
            adSample.errorPosition = this.currentTime
            adSample.errorPercentage = Util.calculatePercentage(adSample.errorPosition, adSample.ad.duration, true)
        }

        adSample.errorCode = code
        adSample.errorMessage = message
        this.completeAd(adBreak, adSample, adSample.errorPosition ?: 0)
    }

    fun onAdManifestLoaded(adBreak: AdBreak, downloadTime: Long) {
        this.adManifestDownloadTimes[adBreak.id] = downloadTime
        if (adBreak.tagType == AdTagType.VMAP) {
            this.sendAnalyticsRequest(adBreak)
        }
    }

    fun onPlay() {
        if (this.analytics.adAdapter != null && this.analytics.adAdapter.isLinearAdActive && this.activeAdSample != null) {
            val timestamp = Util.getElapsedTimestamp()
            this.beginPlayingTimestamp = timestamp
            this.isPlaying = true
        }
    }

    fun onPause() {
        if (this.analytics.adAdapter != null && this.analytics.adAdapter.isLinearAdActive && this.activeAdSample != null) {
            if(this.currentTime != null) {
                this.currentTime = this.currentTime
            }
            this.isPlaying = false
        }
    }

    fun onAdSkipped() {
        val activeAdBreak = this.activeAdBreak ?: return
        val activeAdSample = this.activeAdSample ?: return

        activeAdSample.skipped = 1
        activeAdSample.skipPosition = this.currentTime
        activeAdSample.skipPercentage = Util.calculatePercentage(activeAdSample.skipPosition, activeAdSample.ad.duration, true)

        this.resetActiveAd()
        this.completeAd(activeAdBreak, activeAdSample, activeAdSample.skipPosition)
    }

    fun onAdQuartile(quartile: AdQuartile) {
        val activeAdSample = this.activeAdSample ?: return
        when {
            quartile === AdQuartile.FIRST_QUARTILE -> activeAdSample.quartile1 = 1
            quartile === AdQuartile.MIDPOINT -> activeAdSample.midpoint = 1
            quartile === AdQuartile.THIRD_QUARTILE -> activeAdSample.quartile3 = 1
        }
    }

    private fun startAd(adSample: AdSample) {
        adSample.started = 1
        adSample.timePlayed = 0
        adSample.timeInViewport = 0
        adSample.adPodPosition = this.adPodPosition
        this.beginPlayingTimestamp = Util.getElapsedTimestamp()
        this.isPlaying = true
        this.currentTime = 0
        this.adPodPosition++
    }

    private fun completeAd(adBreak: AdBreak, adSample: AdSample, exitPosition: Long? = 0) {
        adSample.exitPosition = exitPosition
        adSample.timePlayed = exitPosition
        adSample.playPercentage = Util.calculatePercentage(adSample.timePlayed, adSample.ad.duration, true)

        // reset startupTimestamp for the next ad, in case there are multiple ads in one ad break
        this.adStartupTimestamp = Util.getElapsedTimestamp()
        this.isPlaying = false
        this.sendAnalyticsRequest(adBreak, adSample)
    }

    private fun resetActiveAd() {
        this.currentTime = null
        this.activeAdSample = null
    }

    private fun getAdManifestDownloadTime(adBreak: AdBreak?): Long? {
        if(adBreak == null || !adManifestDownloadTimes.containsKey(adBreak.id)) {
            return null
        }
        return adManifestDownloadTimes[adBreak.id]
    }

    private fun sendAnalyticsRequest(adBreak: AdBreak, adSample: AdSample? = null) {
        if(analytics.playerAdapter == null) {
            return
        }

        val eventData = AdEventData()

        eventData.analyticsVersion = BuildConfig.VERSION_NAME
        val moduleInfo = analytics.adAdapter?.moduleInformation
        if(moduleInfo != null) {
            eventData.adModule = moduleInfo.name
            eventData.adModuleVersion = moduleInfo.version
        }
        eventData.manifestDownloadTime = getAdManifestDownloadTime(adBreak)
        eventData.playerStartupTime = 1
        eventData.autoplay = this.analytics.adAdapter.isAutoplayEnabled

        eventData.setEventData(analytics.playerAdapter.createEventData())
        eventData.setAdBreak(adBreak)
        eventData.setAdSample(adSample)

        eventData.time = Util.getTimestamp()
        eventData.adImpressionId = Util.getUUID()
        analytics.sendAdEventData(eventData)
    }
}
