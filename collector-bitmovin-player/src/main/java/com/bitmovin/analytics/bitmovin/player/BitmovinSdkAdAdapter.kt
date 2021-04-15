package com.bitmovin.analytics.bitmovin.player

import android.util.Log
import com.bitmovin.analytics.BitmovinAdAnalytics
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.bitmovin.player.utils.AdBreakMapper
import com.bitmovin.analytics.bitmovin.player.utils.AdMapper
import com.bitmovin.analytics.bitmovin.player.utils.AdQuartileFactory
import com.bitmovin.analytics.data.AdModuleInformation
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent
/**
 * An adapter that maps the Ad Events to the BitmovinAdAnalytics class
 */
class BitmovinSdkAdAdapter(val bitmovinPlayer: Player, val adAnalytics: BitmovinAdAnalytics) : AdAdapter {

    private val adMapper: AdMapper = AdMapper()
    private val adBreakMapper: AdBreakMapper = AdBreakMapper()
    private val adQuartileFactory: AdQuartileFactory = AdQuartileFactory()
    private val TAG = "BitmovinSdkAdAdapter"

    private fun playerEventAdStartedListener(event: PlayerEvent.AdStarted) {
        try {
            val ad = event.ad ?: return
            adAnalytics.onAdStarted(adMapper.fromPlayerAd(ad))
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Started", e)
        }
    }

    private fun playerEventAdFinishedListener(event: PlayerEvent.AdFinished) {
        try {
            adAnalytics.onAdFinished()
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Finished", e)
        }
    }

    private fun playerEventAdBreakStartedListener(event: PlayerEvent.AdBreakStarted) {
        try {
            val adBreak = event.adBreak ?: return
            adAnalytics.onAdBreakStarted(adBreakMapper.fromPlayerAdConfiguration(adBreak))
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Break Started", e)
        }
    }

    private fun playerEventAdBreakFinishedListener(event: PlayerEvent.AdBreakFinished) {
        try {
            adAnalytics.onAdBreakFinished()
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Break Finished", e)
        }
    }

    private fun playerEventAdClickedListener(event: PlayerEvent.AdClicked) {
        try {
            adAnalytics.onAdClicked(event.clickThroughUrl)
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Clicked", e)
        }
    }

    private fun playerEventAdErrorListener(event: PlayerEvent.AdError) {
        try {
            val adConf = event.adConfig ?: return
            adAnalytics.onAdError(
                    adBreakMapper.fromPlayerAdConfiguration(adConf),
                    event.code,
                    event.message)
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Error", e)
        }
    }

    private fun playerEventAdSkippedListener(event: PlayerEvent.AdSkipped) {
        try {
            adAnalytics.onAdSkipped()
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Skipped", e)
        }
    }

    private fun playerEventAdManifestLoadedListener(event: PlayerEvent.AdManifestLoaded) {
        try {
            val adBreak = event.adBreak ?: return
            adAnalytics.onAdManifestLoaded(adBreakMapper.fromPlayerAdConfiguration(adBreak), event.downloadTime)
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Manifest Loaded", e)
        }
    }

    private fun playerEventPlayListener(event: PlayerEvent.Play) {
//        adAnalytics.onPlay()
    }

    private fun playerEventPausedListener(event: PlayerEvent.Paused) {
//        adAnalytics.onPause()
    }

    private fun playerEventAdQuartileListener(event: PlayerEvent.AdQuartile) {
        try {
            adAnalytics.onAdQuartile(adQuartileFactory.FromPlayerAdQuartile(event.quartile))
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Quartile Listener ", e)
        }
    }

    init {
        bitmovinPlayer.on(PlayerEvent.AdStarted::class, ::playerEventAdStartedListener)
        bitmovinPlayer.on(PlayerEvent.AdFinished::class, ::playerEventAdFinishedListener)
        bitmovinPlayer.on(PlayerEvent.AdBreakStarted::class, ::playerEventAdBreakStartedListener)
        bitmovinPlayer.on(PlayerEvent.AdBreakFinished::class, ::playerEventAdBreakFinishedListener)
        bitmovinPlayer.on(PlayerEvent.AdClicked::class, ::playerEventAdClickedListener)
        bitmovinPlayer.on(PlayerEvent.AdError::class, ::playerEventAdErrorListener)
        bitmovinPlayer.on(PlayerEvent.AdSkipped::class, ::playerEventAdSkippedListener)
        bitmovinPlayer.on(PlayerEvent.AdManifestLoaded::class, ::playerEventAdManifestLoadedListener)
        bitmovinPlayer.on(PlayerEvent.Play::class, ::playerEventPlayListener)
        bitmovinPlayer.on(PlayerEvent.Paused::class, ::playerEventPausedListener)
        bitmovinPlayer.on(PlayerEvent.AdQuartile::class, ::playerEventAdQuartileListener)
    }

    override fun release() {
        bitmovinPlayer.off(::playerEventAdStartedListener)
        bitmovinPlayer.off(::playerEventAdFinishedListener)
        bitmovinPlayer.off(::playerEventAdBreakStartedListener)
        bitmovinPlayer.off(::playerEventAdBreakFinishedListener)
        bitmovinPlayer.off(::playerEventAdClickedListener)
        bitmovinPlayer.off(::playerEventAdErrorListener)
        bitmovinPlayer.off(::playerEventAdSkippedListener)
        bitmovinPlayer.off(::playerEventAdManifestLoadedListener)
        bitmovinPlayer.off(::playerEventPlayListener)
        bitmovinPlayer.off(::playerEventPausedListener)
        bitmovinPlayer.off(::playerEventAdQuartileListener)
    }

    override val isLinearAdActive: Boolean
        get() = bitmovinPlayer.isAd
    override val moduleInformation: AdModuleInformation
        // TODO get actual module from player
        get() = AdModuleInformation("DefaultAdvertisingService", BitmovinUtil.getPlayerVersion())
    override val isAutoplayEnabled: Boolean?
        get() = bitmovinPlayer.config.playbackConfig?.isAutoplayEnabled
}
