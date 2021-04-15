package com.bitmovin.analytics.bitmovin.player

import android.util.Log
import com.bitmovin.analytics.BitmovinAdAnalytics
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.bitmovin.player.utils.AdBreakMapper
import com.bitmovin.analytics.bitmovin.player.utils.AdMapper
import com.bitmovin.analytics.bitmovin.player.utils.AdQuartileFactory
import com.bitmovin.analytics.data.AdModuleInformation
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.listener.OnAdBreakFinishedListener
import com.bitmovin.player.api.event.listener.OnAdBreakStartedListener
import com.bitmovin.player.api.event.listener.OnAdClickedListener
import com.bitmovin.player.api.event.listener.OnAdErrorListener
import com.bitmovin.player.api.event.listener.OnAdFinishedListener
import com.bitmovin.player.api.event.listener.OnAdManifestLoadedListener
import com.bitmovin.player.api.event.listener.OnAdQuartileListener
import com.bitmovin.player.api.event.listener.OnAdSkippedListener
import com.bitmovin.player.api.event.listener.OnAdStartedListener
import com.bitmovin.player.api.event.listener.OnPausedListener
import com.bitmovin.player.api.event.listener.OnPlayListener

/**
 * An adapter that maps the Ad Events to the BitmovinAdAnalytics class
 */
class BitmovinSdkAdAdapter(val bitmovinPlayer: BitmovinPlayer, val adAnalytics: BitmovinAdAnalytics) : AdAdapter {

    private val adMapper: AdMapper = AdMapper()
    private val adBreakMapper: AdBreakMapper = AdBreakMapper()
    private val adQuartileFactory: AdQuartileFactory = AdQuartileFactory()
    private val TAG = "BitmovinSdkAdAdapter"

    private val onAdStartedListener = OnAdStartedListener {
        try {
            val ad = it.ad ?: return@OnAdStartedListener
            adAnalytics.onAdStarted(adMapper.fromPlayerAd(ad))
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Started", e)
        }
    }

    private val onAdFinishedListener = OnAdFinishedListener {
        try {
            adAnalytics.onAdFinished()
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Finished", e)
        }
    }

    private val onAdBreakStartedListener = OnAdBreakStartedListener {
        try {
            val adBreak = it.adBreak ?: return@OnAdBreakStartedListener
            adAnalytics.onAdBreakStarted(adBreakMapper.fromPlayerAdConfiguration(adBreak))
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Break Started", e)
        }
    }

    private val onAdBreakFinishedListener = OnAdBreakFinishedListener {
        try {
            adAnalytics.onAdBreakFinished()
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Break Finished", e)
        }
    }

    private val onAdClickedListener = OnAdClickedListener {
        try {
            adAnalytics.onAdClicked(it.clickThroughUrl)
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Clicked", e)
        }
    }

    private val onAdErrorListener = OnAdErrorListener {
        try {
            val adConf = it.adConfiguration ?: return@OnAdErrorListener
            adAnalytics.onAdError(
                    adBreakMapper.fromPlayerAdConfiguration(adConf),
                    it.code,
                    it.message)
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Error", e)
        }
    }

    private val onAdSkippedListener = OnAdSkippedListener {
        try {
            adAnalytics.onAdSkipped()
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Skipped", e)
        }
    }

    private val onAdManifestLoadedListener = OnAdManifestLoadedListener {
        try {
            val adBreak = it.adBreak ?: return@OnAdManifestLoadedListener
            adAnalytics.onAdManifestLoaded(adBreakMapper.fromPlayerAdConfiguration(adBreak), it.downloadTime)
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Manifest Loaded", e)
        }
    }

    private val onPlayListener = OnPlayListener {
//        adAnalytics.onPlay()
    }

    private val onPausedListener = OnPausedListener {
//        adAnalytics.onPause()
    }

    private val onAdQuartileListener = OnAdQuartileListener {
        try {
            adAnalytics.onAdQuartile(adQuartileFactory.FromPlayerAdQuartile(it.quartile))
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Quartile Listener ", e)
        }
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
        bitmovinPlayer.removeEventListener(onAdStartedListener)
        bitmovinPlayer.removeEventListener(onAdFinishedListener)
        bitmovinPlayer.removeEventListener(onAdBreakStartedListener)
        bitmovinPlayer.removeEventListener(onAdBreakFinishedListener)
        bitmovinPlayer.removeEventListener(onAdClickedListener)
        bitmovinPlayer.removeEventListener(onAdErrorListener)
        bitmovinPlayer.removeEventListener(onAdSkippedListener)
        bitmovinPlayer.removeEventListener(onAdManifestLoadedListener)
        bitmovinPlayer.removeEventListener(onPlayListener)
        bitmovinPlayer.removeEventListener(onPausedListener)
        bitmovinPlayer.removeEventListener(onAdQuartileListener)
    }

    override val isLinearAdActive: Boolean
        get() = bitmovinPlayer.isAd
    override val moduleInformation: AdModuleInformation
        // TODO get actual module from player
        get() = AdModuleInformation("DefaultAdvertisingService", BitmovinUtil.getPlayerVersion())
    override val isAutoplayEnabled: Boolean?
        get() = bitmovinPlayer.config.playbackConfiguration?.isAutoplayEnabled
}
