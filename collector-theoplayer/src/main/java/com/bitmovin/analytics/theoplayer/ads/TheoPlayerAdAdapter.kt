package com.bitmovin.analytics.theoplayer.ads

import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.adapters.AdAnalyticsEventListener
import com.bitmovin.analytics.ads.Ad
import com.bitmovin.analytics.ads.AdBreak
import com.bitmovin.analytics.ads.AdQuartile
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.ads.AdBeginEvent
import com.theoplayer.android.api.event.ads.AdBreakBeginEvent
import com.theoplayer.android.api.event.ads.AdBreakEndEvent
import com.theoplayer.android.api.event.ads.AdClickedEvent
import com.theoplayer.android.api.event.ads.AdEndEvent
import com.theoplayer.android.api.event.ads.AdErrorEvent
import com.theoplayer.android.api.event.ads.AdFirstQuartileEvent
import com.theoplayer.android.api.event.ads.AdMidpointEvent
import com.theoplayer.android.api.event.ads.AdSkipEvent
import com.theoplayer.android.api.event.ads.AdThirdQuartileEvent
import com.theoplayer.android.api.event.ads.AdsEventTypes
import com.theoplayer.android.api.event.player.PlayEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.player.Player
import com.theoplayer.android.api.ads.AdBreak as TheoAdBreak

internal class TheoPlayerAdAdapter(
    private val player: Player,
) : AdAdapter {
    private val observableSupport = ObservableSupport<AdAnalyticsEventListener>()
    private var currentAd: Ad? = null
    private var currentTheoAdBreak: TheoAdBreak? = null

    private val adBreakBeginListener =
        EventListener<AdBreakBeginEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad break begin")
                val adBreak = event.adBreak ?: return@EventListener
                currentTheoAdBreak = adBreak
                observableSupport.notify { it.onAdBreakStarted(AdBreakMapper.fromTheoAdBreak(adBreak)) }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Break Begin", e)
            }
        }

    private val adBeginListener =
        EventListener<AdBeginEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad begin")
                val theoAd = event.ad ?: return@EventListener
                if (theoAd.type != "linear") return@EventListener
                val mappedAd = AdMapper.fromTheoAd(theoAd)
                currentAd = mappedAd
                observableSupport.notify { it.onAdStarted(mappedAd) }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Begin", e)
            }
        }

    private val adEndListener =
        EventListener<AdEndEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad end")
                val theoAd = event.ad ?: return@EventListener
                if (theoAd.type != "linear") return@EventListener
                observableSupport.notify { it.onAdFinished() }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad End", e)
            }
        }

    private val adBreakEndListener =
        EventListener<AdBreakEndEvent> { _ ->
            try {
                BitmovinLog.d(TAG, "ad break end")
                currentTheoAdBreak = null
                observableSupport.notify { it.onAdBreakFinished() }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Break End", e)
            }
        }

    private val adSkipListener =
        EventListener<AdSkipEvent> { _ ->
            try {
                BitmovinLog.d(TAG, "ad skipped")
                observableSupport.notify { it.onAdSkipped() }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Skip", e)
            }
        }

    private val adErrorListener =
        EventListener<AdErrorEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad error")
                val theoAdBreak = event.ad?.adBreak ?: currentTheoAdBreak
                val adBreak =
                    if (theoAdBreak != null) {
                        AdBreakMapper.fromTheoAdBreak(theoAdBreak)
                    } else {
                        // error fired before any ad break was created (e.g. early load failure)
                        AdBreak(id = Util.uUID, ads = emptyList())
                    }
                observableSupport.notify {
                    it.onAdError(adBreak, 0, event.error)
                }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Error", e)
            }
        }

    private val adFirstQuartileListener =
        EventListener<AdFirstQuartileEvent> { _ ->
            try {
                BitmovinLog.d(TAG, "ad first quartile")
                observableSupport.notify { it.onAdQuartile(AdQuartile.FIRST_QUARTILE) }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad First Quartile", e)
            }
        }

    private val adMidpointListener =
        EventListener<AdMidpointEvent> { _ ->
            try {
                BitmovinLog.d(TAG, "ad midpoint")
                observableSupport.notify { it.onAdQuartile(AdQuartile.MIDPOINT) }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Midpoint", e)
            }
        }

    private val adThirdQuartileListener =
        EventListener<AdThirdQuartileEvent> { _ ->
            try {
                BitmovinLog.d(TAG, "ad third quartile")
                observableSupport.notify { it.onAdQuartile(AdQuartile.THIRD_QUARTILE) }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Third Quartile", e)
            }
        }

    private val adClickedListener =
        EventListener<AdClickedEvent> { it ->
            try {
                BitmovinLog.d(TAG, "ad clicked")
                observableSupport.notify { it.onAdClicked(null) }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Clicked", e)
            }
        }

    private val playListener =
        EventListener<PlayEvent> {
            observableSupport.notify { it.onPlayEvent() }
        }

    init {
        player.addEventListener(PlayerEventTypes.PLAY, playListener)
        player.ads.addEventListener(AdsEventTypes.AD_BREAK_BEGIN, adBreakBeginListener)
        player.ads.addEventListener(AdsEventTypes.AD_BEGIN, adBeginListener)
        player.ads.addEventListener(AdsEventTypes.AD_END, adEndListener)
        player.ads.addEventListener(AdsEventTypes.AD_BREAK_END, adBreakEndListener)
        player.ads.addEventListener(AdsEventTypes.AD_SKIP, adSkipListener)
        player.ads.addEventListener(AdsEventTypes.AD_ERROR, adErrorListener)
        player.ads.addEventListener(AdsEventTypes.AD_FIRST_QUARTILE, adFirstQuartileListener)
        player.ads.addEventListener(AdsEventTypes.AD_MIDPOINT, adMidpointListener)
        player.ads.addEventListener(AdsEventTypes.AD_THIRD_QUARTILE, adThirdQuartileListener)
        player.ads.addEventListener(AdsEventTypes.AD_CLICKED, adClickedListener)
    }

    override fun release() {
        player.removeEventListener(PlayerEventTypes.PLAY, playListener)
        player.ads.removeEventListener(AdsEventTypes.AD_BREAK_BEGIN, adBreakBeginListener)
        player.ads.removeEventListener(AdsEventTypes.AD_BEGIN, adBeginListener)
        player.ads.removeEventListener(AdsEventTypes.AD_END, adEndListener)
        player.ads.removeEventListener(AdsEventTypes.AD_BREAK_END, adBreakEndListener)
        player.ads.removeEventListener(AdsEventTypes.AD_SKIP, adSkipListener)
        player.ads.removeEventListener(AdsEventTypes.AD_ERROR, adErrorListener)
        player.ads.removeEventListener(AdsEventTypes.AD_FIRST_QUARTILE, adFirstQuartileListener)
        player.ads.removeEventListener(AdsEventTypes.AD_MIDPOINT, adMidpointListener)
        player.ads.removeEventListener(AdsEventTypes.AD_THIRD_QUARTILE, adThirdQuartileListener)
        player.ads.removeEventListener(AdsEventTypes.AD_CLICKED, adClickedListener)
    }

    override val isLinearAdActive: Boolean
        get() = player.ads.isPlaying

    override fun subscribe(listener: AdAnalyticsEventListener) {
        observableSupport.subscribe(listener)
    }

    override fun unsubscribe(listener: AdAnalyticsEventListener) {
        observableSupport.unsubscribe(listener)
    }

    companion object {
        private const val TAG = "TheoPlayerAdAdapter"
    }
}
