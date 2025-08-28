package com.bitmovin.analytics

import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.adapters.AdAnalyticsEventListener
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.ads.Ad
import com.bitmovin.analytics.ads.AdBreak
import com.bitmovin.analytics.ads.AdQuartile
import com.bitmovin.analytics.ads.AdTagType
import com.bitmovin.analytics.data.AdSample
import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.ErrorCode
import com.bitmovin.analytics.dtos.ErrorData
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.internal.InternalBitmovinApi
import com.bitmovin.analytics.utils.ErrorTransformationHelper
import com.bitmovin.analytics.utils.Util

@InternalBitmovinApi
class BitmovinAdAnalytics(private val analytics: BitmovinAnalytics) : AdAnalyticsEventListener {
    private var activeAdBreak: AdBreak? = null
    private var activeAdSample: AdSample? = null
    private var adPodPosition: Int = 0
    private var elapsedTimeAdStartup: Long? = null
    private var elapsedTimeBeginPlaying: Long? = null
    private var isPlaying: Boolean = false
    private val adManifestDownloadTimes: HashMap<String, Long> = hashMapOf()
    private var playerAdapter: PlayerAdapter? = null
    private var adAdapter: AdAdapter? = null

    private var currentTime: Long? = null
        get() =
            if (this.isPlaying) {
                if (field == null || this.elapsedTimeBeginPlaying == null) {
                    null
                } else {
                    field!! + Util.elapsedTime - this.elapsedTimeBeginPlaying!!
                }
            } else {
                field
            }

    fun attachAdapter(
        playerAdapter: PlayerAdapter,
        adAdapter: AdAdapter,
    ) {
        this.playerAdapter = playerAdapter
        this.adAdapter = adAdapter
        this.adAdapter?.subscribe(this)
    }

    fun detachAdapter() {
        this.playerAdapter = null
        this.adAdapter?.unsubscribe(this)
        this.adAdapter?.release()
        this.adAdapter = null
    }

    override fun onAdStarted(ad: Ad) {
        if (!ad.isLinear) {
            return
        }

        this.resetActiveAd()
        val adSample = AdSample(ad = ad)
        val elapsedTimeAdStartup = this.elapsedTimeAdStartup
        adSample.adStartupTime = if (elapsedTimeAdStartup != null) Util.elapsedTime - elapsedTimeAdStartup else null

        this.activeAdSample = adSample

        this.startAd(adSample)
    }

    override fun onAdFinished() {
        val activeAdSample = this.activeAdSample ?: return
        val activeAdBreak = this.activeAdBreak ?: return

        val adSample = activeAdSample.copy()
        adSample.completed = 1
        this.resetActiveAd()
        this.completeAd(activeAdBreak, adSample, adSample.ad.duration)
    }

    override fun onAdBreakStarted(adBreak: AdBreak) {
        this.adPodPosition = 0
        this.activeAdBreak = adBreak
        this.elapsedTimeAdStartup = Util.elapsedTime
    }

    override fun onAdBreakFinished() {
        this.resetActiveAd()
        this.activeAdBreak = null
    }

    override fun onAdClicked(clickThroughUrl: String?) {
        val activeAdSample = this.activeAdSample ?: return

        activeAdSample.ad.clickThroughUrl = clickThroughUrl
        activeAdSample.clicked = 1
        activeAdSample.clickPosition = this.currentTime
        activeAdSample.clickPercentage = Util.calculatePercentage(activeAdSample.clickPosition, activeAdSample.ad.duration, true)
    }

    override fun onAdError(
        adBreak: AdBreak,
        code: Int,
        message: String?,
    ) {
        val adSample = this.activeAdSample ?: AdSample()

        if (adSample.ad.id != null && adBreak.ads.any { ad -> ad.id == adSample.ad.id }) {
            adSample.errorPosition = this.currentTime
            adSample.errorPercentage = Util.calculatePercentage(adSample.errorPosition, adSample.ad.duration, true)
        }

        val errorCode = ErrorCode(code, message ?: "", ErrorData())
        val transformedError =
            ErrorTransformationHelper.transformErrorWithUserCallback(
                analytics.config.errorTransformerCallback,
                errorCode,
                null,
            )

        adSample.errorCode = transformedError.errorCode
        adSample.errorMessage = transformedError.message
        adSample.errorSeverity = transformedError.errorSeverity
        this.completeAd(adBreak, adSample, adSample.errorPosition ?: 0)
    }

    override fun onAdManifestLoaded(
        adBreak: AdBreak,
        downloadTime: Long,
    ) {
        this.adManifestDownloadTimes[adBreak.id] = downloadTime
        if (adBreak.tagType == AdTagType.VMAP) {
            this.sendAnalyticsRequest(adBreak)
        }
    }

    override fun onPlay() {
        if (adAdapter?.isLinearAdActive == true && this.activeAdSample != null) {
            val elapsedTime = Util.elapsedTime
            this.elapsedTimeBeginPlaying = elapsedTime
            this.isPlaying = true
        }
    }

    override fun onPause() {
        if (adAdapter?.isLinearAdActive == true && this.activeAdSample != null) {
            if (this.currentTime != null) {
                this.currentTime = this.currentTime
            }
            this.isPlaying = false
        }
    }

    override fun onAdSkipped() {
        val activeAdBreak = this.activeAdBreak ?: return
        val activeAdSample = this.activeAdSample ?: return

        activeAdSample.skipped = 1
        activeAdSample.skipPosition = this.currentTime
        activeAdSample.skipPercentage = Util.calculatePercentage(activeAdSample.skipPosition, activeAdSample.ad.duration, true)

        this.resetActiveAd()
        this.completeAd(activeAdBreak, activeAdSample, activeAdSample.skipPosition)
    }

    override fun onAdQuartile(quartile: AdQuartile) {
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
        this.elapsedTimeBeginPlaying = Util.elapsedTime
        this.isPlaying = true
        this.currentTime = 0
        this.adPodPosition++
    }

    private fun completeAd(
        adBreak: AdBreak,
        adSample: AdSample,
        exitPosition: Long? = 0,
    ) {
        adSample.exitPosition = exitPosition
        adSample.timePlayed = exitPosition
        adSample.playPercentage = Util.calculatePercentage(adSample.timePlayed, adSample.ad.duration, true)

        // reset elapsedTimeAdStartup for the next ad, in case there are multiple ads in one ad break
        this.elapsedTimeAdStartup = Util.elapsedTime
        this.isPlaying = false
        this.sendAnalyticsRequest(adBreak, adSample)
    }

    private fun resetActiveAd() {
        this.currentTime = null
        this.activeAdSample = null
    }

    private fun getAdManifestDownloadTime(adBreak: AdBreak?): Long? {
        if (adBreak == null || !adManifestDownloadTimes.containsKey(adBreak.id)) {
            return null
        }
        return adManifestDownloadTimes[adBreak.id]
    }

    private fun sendAnalyticsRequest(
        adBreak: AdBreak,
        adSample: AdSample? = null,
    ) {
        val eventData = playerAdapter?.createEventDataForAdSample() ?: return
        val adEventData = AdEventData.fromEventData(eventData, AdType.CLIENT_SIDE)

        adEventData.analyticsVersion = Util.analyticsVersion
        val moduleInfo = adAdapter?.moduleInformation
        if (moduleInfo != null) {
            adEventData.adModule = moduleInfo.name
            adEventData.adModuleVersion = moduleInfo.version
        }
        adEventData.manifestDownloadTime = getAdManifestDownloadTime(adBreak)
        adEventData.playerStartupTime = 1
        adEventData.autoplay = adAdapter?.isAutoplayEnabled

        adEventData.setAdBreak(adBreak)
        adEventData.setAdSample(adSample)
        adEventData.adImpressionId = Util.uUID
        analytics.sendAdEventData(adEventData)
    }
}
