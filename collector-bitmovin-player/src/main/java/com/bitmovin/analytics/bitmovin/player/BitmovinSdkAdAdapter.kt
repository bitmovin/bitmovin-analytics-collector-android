package com.bitmovin.analytics.bitmovin.player

import android.util.Log
import com.bitmovin.analytics.BitmovinAdAnalytics
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.data.AdModuleInformation
import com.bitmovin.analytics.stateMachines.PlayerState
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.data.AdStartedEvent
import com.bitmovin.player.api.event.data.StallStartedEvent
import com.bitmovin.player.api.event.listener.*

class BitmovinSdkAdAdapter(val bitmovinPlayer: BitmovinPlayer, val adAnalytics: BitmovinAdAnalytics) : AdAdapter {

    private val onAdStartedListener = OnAdStartedListener {
        adAnalytics.onAdStarted()
    }

    private val onAdFinishedListener = OnAdFinishedListener {
        adAnalytics.onAdFinished()
    }

    private val onAdBreakStartedListener = OnAdBreakStartedListener {
        adAnalytics.onAdBreakStarted()
    }

    private val onAdBreakFinishedListener = OnAdBreakFinishedListener {
        adAnalytics.onAdBreakFinished()
    }

    private val onAdClickedListener = OnAdClickedListener {
        adAnalytics.onAdClicked(it.clickThroughUrl)
    }

    private val onAdErrorListener = OnAdErrorListener {
        adAnalytics.onAdError(null, it.code, it.message)
    }

    private val onAdSkippedListener = OnAdSkippedListener {
        adAnalytics.onAdSkipped()
    }

    private val onAdManifestLoadedListener = OnAdManifestLoadedListener {
        adAnalytics.onAdManifestLoaded()
    }

    private val onPlayListener = OnPlayListener {
        adAnalytics.onPlay()
    }

    private val onPausedListener = OnPausedListener {
        adAnalytics.onPause()
    }

    private val onAdQuartileListener = OnAdQuartileListener {
        adAnalytics.onAdQuartile()
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