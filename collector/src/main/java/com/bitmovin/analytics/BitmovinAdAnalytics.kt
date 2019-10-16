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
    private var currentTime: Long? = null
    private var beginPlayingTimestamp: Long? = null
    private var isPlaying: Boolean = false
    private val adManifestDownloadTimes: HashMap<String, Long?> = hashMapOf()

    fun onAdStarted(ad: Ad) {
        if (ad.isLinear != true) {
            return
        }

        this.resetActiveAd()
        this.activeAdSample = AdSample(ad = ad)
        this.currentTime = null
        this.activeAdSample!!.adStartupTime = if (this.adStartupTimestamp != null) Util.getTimeStamp() - this.adStartupTimestamp!! else null

        this.startAd(this.activeAdSample!!)
    }

    fun onAdFinished() {
        if (this.activeAdBreak == null || this.activeAdSample == null) {
            return
        }

        val adSample = this.activeAdSample!!.copy()
        adSample.completed = 1
        this.resetActiveAd()
        this.completeAd(this.activeAdBreak!!, adSample, adSample.ad.duration)
    }

    fun onAdBreakStarted(adBreak: AdBreak) {
        this.adPodPosition = 0
        this.activeAdBreak = adBreak
        this.adStartupTimestamp = Util.getTimeStamp()
    }

    fun onAdBreakFinished() {
        this.resetActiveAd()
        this.activeAdBreak = null
    }

    fun onAdClicked(clickThroughUrl: String?) {
        if (this.activeAdSample == null) {
            return
        }
        this.activeAdSample!!.ad.clickThroughUrl = clickThroughUrl
        this.activeAdSample!!.clicked = 1
        this.activeAdSample!!.clickPosition = this.currentTime
        this.activeAdSample!!.clickPercentage = Util.calculatePercentage(this.activeAdSample!!.clickPosition, this.activeAdSample!!.ad.duration)
    }

    fun onAdError(adBreak: AdBreak, code: Int?, message: String?) {
        val adSample: AdSample
        //todo change to references here
        if (this.activeAdSample != null &&
                adBreak.ads.any { ad -> ad.id == this.activeAdSample!!.ad.id }) {
            adSample = this.activeAdSample!!
            adSample.errorPosition = this.currentTime
            adSample.errorPercentage = Util.calculatePercentage(adSample.errorPosition, adSample.ad.duration)
        } else {
            adSample = AdSample()
        }
        adSample.errorCode = code
        // TODO adBreakSample.errorData = JSON.stringify(event.data)
        adSample.errorMessage = message
        this.completeAd(adBreak, adSample, adSample.errorPosition);
    }

    fun onAdManifestLoaded(adBreak: AdBreak, downloadTime: Long?) {
        // TODO test if all have an id, otherwise pass the downloadTime in the sendAnalyticsRequest
        this.adManifestDownloadTimes[adBreak.id] = downloadTime
        if (adBreak.tagType == AdTagType.VMAP) {
            this.sendAnalyticsRequest(adBreak)
        }
    }

    fun onPlay() {
        if (this.analytics.adAdapter != null && this.analytics.adAdapter.isLinearAdActive && this.activeAdSample != null) {
            val timestamp = Util.getTimeStamp()
            this.beginPlayingTimestamp = timestamp
            // TODO this.enterViewportTimestamp = timestamp
            this.isPlaying = true
        }
    }

    fun onPause() {
        if (this.analytics.adAdapter != null && this.analytics.adAdapter.isLinearAdActive && this.activeAdSample != null) {
            this.updatePlayingTime(this.activeAdSample!!)
            this.isPlaying = false
        }
    }

    fun onAdSkipped() {
        if (this.activeAdBreak == null || this.activeAdSample == null) {
            return;
        }

        val adSample = this.activeAdSample!!.copy()
        adSample.skipped = 1
        adSample.skipPosition = this.currentTime
        adSample.skipPercentage = Util.calculatePercentage(adSample.skipPosition, adSample.ad.duration)
        this.resetActiveAd()
        this.completeAd(this.activeAdBreak!!, adSample, adSample.skipPosition)
    }

    fun onAdQuartile(quartile: AdQuartile) {
        if (this.activeAdSample == null) {
            return
        }
        when {
            quartile === AdQuartile.FIRST_QUARTILE -> this.activeAdSample!!.quartile1 = 1
            quartile === AdQuartile.MIDPOINT -> this.activeAdSample!!.midpoint = 1
            quartile === AdQuartile.THIRD_QUARTILE -> this.activeAdSample!!.quartile3 = 1
        }
    }

    private fun startAd(adSample: AdSample) {
        adSample.started = 1
        adSample.timePlayed = 0
        adSample.timeInViewport = 0
        adSample.adPodPosition = this.adPodPosition
        val timestamp = Util.getTimeStamp()
        this.beginPlayingTimestamp = timestamp
        // TODO this.enterViewportTimestamp = this.isContainerInViewport() ? timestamp : undefined;
        this.isPlaying = true
        this.currentTime = 0
        this.adPodPosition++
        // TODO
//        this.currentTimeInterval = window.setInterval(() => {
//            try {
//                if (adSample &&
//                        adSample.adDuration !== undefined &&
//                        adSample.adDuration > 0 &&
//                        this.adapter.isLinearAdActive()) {
//                    this.currentTime = Utils.calculateTime(Math.max(this.adapter.currentTime(), 0));
//                }
//            } catch (e) {
//                logger.log('AdStarted monitoring interval failed and got cleared', e);
//                this.resetActiveAd();
//            }
//        }, AdAnalytics.TIMEOUT_CURRENT_TIME_INTERVAL);
    }

    private fun completeAd(adBreak: AdBreak, adSample: AdSample?, exitPosition: Long? = null) {
        var adSample = adSample ?: AdSample()
        adSample.exitPosition = exitPosition
        adSample.playPercentage = Util.calculatePercentage(adSample.exitPosition, adSample.ad.duration)

        // reset startupTimestamp for the next ad, in case there are multiple ads in one ad break
        this.adStartupTimestamp = Util.getTimeStamp()
        this.updatePlayingTime(adSample)
        this.isPlaying = false
        this.sendAnalyticsRequest(adBreak, adSample)
    }

    private fun resetActiveAd() {
        // TODO window.clearInterval(this.currentTimeInterval)
        this.currentTime = null
        this.activeAdSample = null
    }

    private fun getAdManifestDownloadTime(adBreak: AdBreak?): Long? {
        if(adBreak == null || !adManifestDownloadTimes.containsKey(adBreak.id)) {
            return null
        }
        return adManifestDownloadTimes[adBreak.id]
    }

    private fun updatePlayingTime(adSample: AdSample) {
        val timestamp = Util.getTimeStamp()
        if (this.beginPlayingTimestamp != null && this.isPlaying) {
            if (adSample.timePlayed != null) {
                adSample.timePlayed = adSample.timePlayed!! + timestamp - this.beginPlayingTimestamp!!
            }
            // TODO
//            if (this.isContainerInViewport() &&
//                    this.enterViewportTimestamp &&
//                    adSample.timeInViewport !== undefined) {
//                adSample.timeInViewport += timestamp - this.enterViewportTimestamp;
//            }
        }
    }

    private fun sendAnalyticsRequest(adBreak: AdBreak, adSample: AdSample? = null) {
        if(analytics.playerAdapter == null) {
            return
        }

        val eventData = AdEventData()

        eventData.analyticsVersion = Util.getVersion()
        val moduleInfo = analytics.adAdapter?.moduleInformation
        if(moduleInfo != null) {
            eventData.adModule = moduleInfo.name
            eventData.adModuleVersion = moduleInfo.version
        }
        eventData.manifestDownloadTime = getAdManifestDownloadTime(adBreak)
        eventData.playerStartupTime = 1
        // TODO missing
        // eventData.pageLoadTime
        // eventData.autoplay
        // eventData.pageLoadType

        eventData.setEventData(analytics.playerAdapter.createEventData())
        eventData.setAdBreak(adBreak)
        eventData.setAdSample(adSample)

        eventData.time = Util.getTimeStamp()
        eventData.adImpressionId = Util.getUUID()
        // TODO eventData.percentageInViewport
        analytics.sendAdEventData(eventData)
    }
}
