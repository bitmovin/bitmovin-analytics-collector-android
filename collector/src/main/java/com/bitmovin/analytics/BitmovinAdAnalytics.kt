package com.bitmovin.analytics

import com.bitmovin.analytics.data.AdBreakData
import com.bitmovin.analytics.data.AdData
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.utils.Util

class BitmovinAdAnalytics(var analytics: BitmovinAnalytics) {
    fun onAdStarted() {

    }

    fun onAdFinished() {

    }

    fun onAdBreakStarted() {

    }

    fun onAdBreakFinished() {

    }

    fun onAdClicked() {

    }

    fun onAdError() {

    }

    fun onAdManifestLoaded() {

    }

    fun onPlay() {

    }

    fun onPause() {

    }

    fun onAdSkipped() {

    }

    fun onAdQuartile() {

    }

    private fun sendAnalyticsRequest(adBreakData: AdBreakData, adData: AdData?) {
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
        eventData.playerStartupTime = 1
        // TODO missing
        // eventData.pageLoadTime
        // eventData.autoplay
        // eventData.pageLoadType

        eventData.setEventData(analytics.playerAdapter.createEventData())
        eventData.setAdBreakData(adBreakData)
        eventData.setAdData(adData)

        eventData.time = Util.getTimeStamp()
        eventData.adImpressionId = Util.getUUID()
        // TODO eventData.percentageInViewport
        analytics.sendAdEventData(eventData)
    }
}