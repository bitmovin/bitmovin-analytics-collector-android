package com.bitmovin.analytics.bitmovin.player

import android.util.Log
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.adapters.AdAnalyticsEventListener
import com.bitmovin.analytics.bitmovin.player.utils.AdBreakMapper
import com.bitmovin.analytics.bitmovin.player.utils.AdMapper
import com.bitmovin.analytics.bitmovin.player.utils.AdQuartileFactory
import com.bitmovin.analytics.data.AdModuleInformation
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.PlayerEvent

/**
 * An adapter that maps the Ad Events to the BitmovinAdAnalytics class
 */
internal class BitmovinSdkAdAdapter(val bitmovinPlayer: Player) : AdAdapter {
    private val observableSupport = ObservableSupport<AdAnalyticsEventListener>()
    private val adMapper: AdMapper = AdMapper()
    private val adBreakMapper: AdBreakMapper = AdBreakMapper()
    private val adQuartileFactory: AdQuartileFactory = AdQuartileFactory()
    private val TAG = "BitmovinSdkAdAdapter"

    private val playerEventAdStartedListener: (PlayerEvent.AdStarted) -> Unit = method@{ event ->
        try {
            val ad = event.ad ?: return@method
            observableSupport.notify { it.onAdStarted(adMapper.fromPlayerAd(ad)) }
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Started", e)
        }
    }

    private val playerEventAdFinishedListener: (PlayerEvent.AdFinished) -> Unit = {
        try {
            observableSupport.notify { it.onAdFinished() }
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Finished", e)
        }
    }

    private val playerEventAdBreakStartedListener: (PlayerEvent.AdBreakStarted) -> Unit = method@{ event ->
        try {
            val adBreak = event.adBreak ?: return@method
            observableSupport.notify { it.onAdBreakStarted(adBreakMapper.fromPlayerAdConfiguration(adBreak)) }
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Break Started", e)
        }
    }

    private val playerEventAdBreakFinishedListener: (PlayerEvent.AdBreakFinished) -> Unit = {
        try {
            observableSupport.notify { it.onAdBreakFinished() }
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Break Finished", e)
        }
    }

    private val playerEventAdClickedListener: (PlayerEvent.AdClicked) -> Unit = { event ->
        try {
            observableSupport.notify { it.onAdClicked(event.clickThroughUrl) }
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Clicked", e)
        }
    }

    private val playerEventAdErrorListener: (PlayerEvent.AdError) -> Unit = method@{ event ->
        try {
            val adConf = event.adConfig ?: return@method
            observableSupport.notify {
                it.onAdError(
                    adBreakMapper.fromPlayerAdConfiguration(adConf),
                    event.code,
                    event.message,
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Error", e)
        }
    }

    private val playerEventAdSkippedListener: (PlayerEvent.AdSkipped) -> Unit = {
        try {
            observableSupport.notify { it.onAdSkipped() }
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Skipped", e)
        }
    }

    private val playerEventAdManifestLoadedListener: (PlayerEvent.AdManifestLoaded) -> Unit = method@{ event ->
        try {
            val adBreak = event.adBreak ?: return@method
            observableSupport.notify { it.onAdManifestLoaded(adBreakMapper.fromPlayerAdConfiguration(adBreak), event.downloadTime) }
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Manifest Loaded", e)
        }
    }

    private val playerEventPlayListener: (PlayerEvent.Play) -> Unit = {
// TODO: why is this commented out??
//        adAnalytics.onPlay()
    }

    private val playerEventPausedListener: (PlayerEvent.Paused) -> Unit = {
// TODO: why is this commented out??
//        adAnalytics.onPause()
    }

    private val playerEventAdQuartileListener: (PlayerEvent.AdQuartile) -> Unit = { event ->
        try {
            observableSupport.notify { it.onAdQuartile(adQuartileFactory.FromPlayerAdQuartile(event.quartile)) }
        } catch (e: Exception) {
            Log.d(TAG, "On Ad Quartile Listener ", e)
        }
    }

    init {
        bitmovinPlayer.on(PlayerEvent.AdStarted::class, playerEventAdStartedListener)
        bitmovinPlayer.on(PlayerEvent.AdFinished::class, playerEventAdFinishedListener)
        bitmovinPlayer.on(PlayerEvent.AdBreakStarted::class, playerEventAdBreakStartedListener)
        bitmovinPlayer.on(PlayerEvent.AdBreakFinished::class, playerEventAdBreakFinishedListener)
        bitmovinPlayer.on(PlayerEvent.AdClicked::class, playerEventAdClickedListener)
        bitmovinPlayer.on(PlayerEvent.AdError::class, playerEventAdErrorListener)
        bitmovinPlayer.on(PlayerEvent.AdSkipped::class, playerEventAdSkippedListener)
        bitmovinPlayer.on(PlayerEvent.AdManifestLoaded::class, playerEventAdManifestLoadedListener)
        bitmovinPlayer.on(PlayerEvent.Play::class, playerEventPlayListener)
        bitmovinPlayer.on(PlayerEvent.Paused::class, playerEventPausedListener)
        bitmovinPlayer.on(PlayerEvent.AdQuartile::class, playerEventAdQuartileListener)
    }

    override fun release() {
        bitmovinPlayer.off(playerEventAdStartedListener)
        bitmovinPlayer.off(playerEventAdFinishedListener)
        bitmovinPlayer.off(playerEventAdBreakStartedListener)
        bitmovinPlayer.off(playerEventAdBreakFinishedListener)
        bitmovinPlayer.off(playerEventAdClickedListener)
        bitmovinPlayer.off(playerEventAdErrorListener)
        bitmovinPlayer.off(playerEventAdSkippedListener)
        bitmovinPlayer.off(playerEventAdManifestLoadedListener)
        bitmovinPlayer.off(playerEventPlayListener)
        bitmovinPlayer.off(playerEventPausedListener)
        bitmovinPlayer.off(playerEventAdQuartileListener)
    }

    override val isLinearAdActive: Boolean
        get() = bitmovinPlayer.isAd
    override val moduleInformation: AdModuleInformation
        get() = AdModuleInformation("DefaultAdvertisingService", BitmovinUtil.playerVersion)
    override val isAutoplayEnabled: Boolean?
        get() = bitmovinPlayer.config.playbackConfig?.isAutoplayEnabled

    override fun subscribe(listener: AdAnalyticsEventListener) {
        observableSupport.subscribe(listener)
    }

    override fun unsubscribe(listener: AdAnalyticsEventListener) {
        observableSupport.unsubscribe(listener)
    }
}
