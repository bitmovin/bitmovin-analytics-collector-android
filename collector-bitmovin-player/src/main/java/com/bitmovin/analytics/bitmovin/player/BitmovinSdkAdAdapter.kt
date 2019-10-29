package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.BitmovinAdAnalytics
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.bitmovin.player.utils.AdBreakFactory
import com.bitmovin.analytics.bitmovin.player.utils.AdFactory
import com.bitmovin.analytics.bitmovin.player.utils.AdQuartileFactory
import com.bitmovin.analytics.data.AdModuleInformation
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.*

/**
 * An adapter that maps the Ad Events to the BitmovinAdAnalytics class
 */
class BitmovinSdkAdAdapter(val bitmovinPlayer: BitmovinPlayer, val adAnalytics: BitmovinAdAnalytics) : AdAdapter {

    private val adFactory: AdFactory = AdFactory()
    private val adBreakFactory: AdBreakFactory = AdBreakFactory()
    private val adQuartileFactory: AdQuartileFactory = AdQuartileFactory()


    private val onAdStartedListener = OnAdStartedListener {
        if(it.ad != null){
            adAnalytics.onAdStarted(adFactory.FromPlayerAd(it.ad!!))
        }
    }

    private val onAdFinishedListener = OnAdFinishedListener {
        adAnalytics.onAdFinished()
    }

    private val onAdBreakStartedListener = OnAdBreakStartedListener {
        if (it.adBreak != null){
            adAnalytics.onAdBreakStarted(adBreakFactory.FromPlayerImaAdBreak(it.adBreak!! as com.bitmovin.player.model.advertising.ima.ImaAdBreak))
        }
    }

    private val onAdBreakFinishedListener = OnAdBreakFinishedListener {
        adAnalytics.onAdBreakFinished()
    }

    private val onAdClickedListener = OnAdClickedListener {
        adAnalytics.onAdClicked(it.clickThroughUrl)
    }

    private val onAdErrorListener = OnAdErrorListener {
        if(it.adConfiguration != null && it.adConfiguration!! is com.bitmovin.player.model.advertising.ima.ImaAdBreak)
        {
            adAnalytics.onAdError(
                    adBreakFactory.FromPlayerImaAdBreak(it.adConfiguration!! as com.bitmovin.player.model.advertising.ima.ImaAdBreak),
                    it.code,
                    it.message)
        }
    }

    private val onAdSkippedListener = OnAdSkippedListener {
        adAnalytics.onAdSkipped()
    }

    private val onAdManifestLoadedListener = OnAdManifestLoadedListener {
        if (it.adBreak != null){
            adAnalytics.onAdManifestLoaded(adBreakFactory.FromPlayerImaAdBreak(it.adBreak!! as com.bitmovin.player.model.advertising.ima.ImaAdBreak), it.downloadTime)
        }
    }

    private val onPlayListener = OnPlayListener {
        adAnalytics.onPlay()
    }

    private val onPausedListener = OnPausedListener {
        adAnalytics.onPause()
    }

    private val onAdQuartileListener = OnAdQuartileListener {
        adAnalytics.onAdQuartile(adQuartileFactory.FromPlayerAdQuartile(it.quartile))
    }

    init {
        bitmovinPlayer.addEventListener(onAdStartedListener)
        bitmovinPlayer.addEventListener(onAdFinishedListener)
        bitmovinPlayer.addEventListener(onAdBreakStartedListener)
        bitmovinPlayer.addEventListener(onAdBreakFinishedListener)
        bitmovinPlayer.addEventListener(onAdClickedListener)
        bitmovinPlayer.addEventListener(onAdErrorListener)
        bitmovinPlayer.addEventListener(onAdSkippedListener)
        bitmovinPlayer.addEventListener(onAdManifestLoadedListener)
        bitmovinPlayer.addEventListener(onPlayListener)
        bitmovinPlayer.addEventListener(onPausedListener)
        bitmovinPlayer.addEventListener(onAdQuartileListener)
    }

    override fun release() {

    }

    override val isLinearAdActive: Boolean
        get() = bitmovinPlayer.isAd
    override val moduleInformation: AdModuleInformation
        // TODO get actual module from player
        get() = AdModuleInformation("DefaultAdvertisingService", BitmovinUtil.getPlayerVersion())
}