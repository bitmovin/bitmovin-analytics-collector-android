package com.bitmovin.analytics.theoplayer.ads

import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.adapters.AdAnalyticsEventListener
import com.bitmovin.analytics.ads.Ad
import com.bitmovin.analytics.ads.AdBreak
import com.bitmovin.analytics.ads.AdQuartile
import com.bitmovin.analytics.api.ads.AdBreakMetadata
import com.bitmovin.analytics.api.ads.AdMetadata
import com.bitmovin.analytics.api.ads.AdQuartileMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdQuartile
import com.bitmovin.analytics.api.ssai.SsaiApi
import com.bitmovin.analytics.theoplayer.TheoPlayerUtils
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.THEOplayerGlobal
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
import java.time.Duration
import com.theoplayer.android.api.ads.AdBreak as TheoAdBreak

internal class TheoPlayerAdAdapter(
    private val player: Player,
    private val ssaiApi: SsaiApi,
) : AdAdapter {
    private val observableSupport = ObservableSupport<AdAnalyticsEventListener>()
    private var currentAd: Ad? = null
    private var currentTheoAdBreak: TheoAdBreak? = null
    private val adBreakBeginListener =
        EventListener<AdBreakBeginEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad break begin")
                val nativeAdBreak = event.adBreak ?: return@EventListener
                currentTheoAdBreak = nativeAdBreak
                val extractedAdBreakMetadata = AdBreakMapper.fromTheoAdBreak(nativeAdBreak)

                if (TheoPlayerUtils.isClientSideAd(nativeAdBreak.integration)) {
                    observableSupport.notify { it.onAdBreakStarted(extractedAdBreakMetadata) }
                } else {
                    val adBreakMetadata =
                        AdBreakMetadata.Builder()
                            .setAdPosition(extractedAdBreakMetadata.position?.mapToSsaiAdPosition())
                            .build()
                    ssaiApi.adBreakStart(adBreakMetadata)
                }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Break Begin", e)
            }
        }

    private val adBeginListener =
        EventListener<AdBeginEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad begin")
                val theoAd = event.ad ?: return@EventListener

                // non-linear ads are not tracked
                if (theoAd.type != "linear") return@EventListener
                val mappedAd = AdMapper.fromTheoAd(theoAd)
                currentAd = mappedAd

                if (TheoPlayerUtils.isClientSideAd(theoAd.integration)) {
                    observableSupport.notify { it.onAdStarted(mappedAd) }
                } else {
                    val ssaiAdMetadata =
                        AdMetadata.Builder()
                            .setAdId(mappedAd.id)
                            .setAdSystem(mappedAd.adSystemName)
                            .setUniversalAdIdValue(mappedAd.universalAdIdValue)
                            .setUniversalAdIdRegistry(mappedAd.universalAdIdRegistry)
                            .setCreativeId(mappedAd.creativeId)
                            .setCreativeAdId(mappedAd.creativeAdId)
                            .setAdvertiserName(mappedAd.advertiserName)
                            .setTitle(mappedAd.title)
                            .setDuration(mappedAd.duration?.let { Duration.ofMillis(it) })
                            .build()
                    ssaiApi.adStart(ssaiAdMetadata)
                }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Begin", e)
            }
        }

    private val adEndListener =
        EventListener<AdEndEvent> { event ->
            try {
                // according to NFL, adEnd is signaling completion of an ad (for ssai and csai)
                BitmovinLog.d(TAG, "ad end")
                val theoAd = event.ad ?: return@EventListener

                // non-linear ads are not tracked
                if (theoAd.type != "linear") return@EventListener

                if (TheoPlayerUtils.isClientSideAd(theoAd.integration)) {
                    observableSupport.notify { it.onAdFinished() }
                } else {
                    ssaiApi.adQuartileFinished(SsaiAdQuartile.COMPLETED, AdQuartileMetadata.Builder().build())
                }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad End", e)
            }
        }

    private val adBreakEndListener =
        EventListener<AdBreakEndEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad break end")
                currentTheoAdBreak = null

                if (TheoPlayerUtils.isClientSideAd(event.adBreak.integration)) {
                    observableSupport.notify { it.onAdBreakFinished() }
                } else {
                    ssaiApi.adBreakEnd()
                }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Break End", e)
            }
        }

    private val adSkipListener =
        EventListener<AdSkipEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad skipped")
                val theoAd = event.ad ?: return@EventListener

                if (TheoPlayerUtils.isClientSideAd(theoAd.integration)) {
                    observableSupport.notify { it.onAdSkipped() }
                }
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
                        // FIXME: how should we handle this? opt for CSAI or SSAI?
                        AdBreak(id = Util.uUID, ads = emptyList())
                    }

                if (TheoPlayerUtils.isClientSideAd(event.ad?.integration)) {
                    observableSupport.notify {
                        it.onAdError(adBreak, 0, event.error)
                    }
                } else {
                    // TODO: handle error for SSAI usecase
                }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Error", e)
            }
        }

    private val adFirstQuartileListener =
        EventListener<AdFirstQuartileEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad first quartile")
                if (TheoPlayerUtils.isClientSideAd(event.ad?.integration)) {
                    observableSupport.notify { it.onAdQuartile(AdQuartile.FIRST_QUARTILE) }
                } else {
                    ssaiApi.adQuartileFinished(SsaiAdQuartile.FIRST, AdQuartileMetadata.Builder().build())
                }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad First Quartile", e)
            }
        }

    private val adMidpointListener =
        EventListener<AdMidpointEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad midpoint")

                if (TheoPlayerUtils.isClientSideAd(event.ad?.integration)) {
                    observableSupport.notify { it.onAdQuartile(AdQuartile.MIDPOINT) }
                } else {
                    ssaiApi.adQuartileFinished(SsaiAdQuartile.MIDPOINT, AdQuartileMetadata.Builder().build())
                }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Midpoint", e)
            }
        }

    private val adThirdQuartileListener =
        EventListener<AdThirdQuartileEvent> { event ->
            try {
                BitmovinLog.d(TAG, "ad third quartile")

                if (TheoPlayerUtils.isClientSideAd(event.ad?.integration)) {
                    observableSupport.notify { it.onAdQuartile(AdQuartile.THIRD_QUARTILE) }
                } else {
                    ssaiApi.adQuartileFinished(SsaiAdQuartile.THIRD, AdQuartileMetadata.Builder().build())
                }
            } catch (e: Exception) {
                BitmovinLog.e(TAG, "On Ad Third Quartile", e)
            }
        }

    private val adClickedListener =
        EventListener<AdClickedEvent> { it ->
            try {
                BitmovinLog.d(TAG, "ad clicked")

                if (TheoPlayerUtils.isClientSideAd(it.ad?.integration)) {
                    observableSupport.notify { it.onAdClicked(null) }
                }
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

        if (supportsAdClickedEvent(THEOplayerGlobal.getVersion())) {
            try {
                player.ads.addEventListener(AdsEventTypes.AD_CLICKED, adClickedListener)
            } catch (_: Exception) {
                // ignore, added for safety reasons
            }
        }
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

        if (supportsAdClickedEvent(THEOplayerGlobal.getVersion())) {
            try {
                player.ads.removeEventListener(AdsEventTypes.AD_CLICKED, adClickedListener)
            } catch (_: Exception) {
                // ignore, added for safety reasons
            }
        }
    }

    override val isLinearAdActive: Boolean
        get() = player.ads.isPlaying

    override fun subscribe(listener: AdAnalyticsEventListener) {
        observableSupport.subscribe(listener)
    }

    override fun unsubscribe(listener: AdAnalyticsEventListener) {
        observableSupport.unsubscribe(listener)
    }

    private fun supportsAdClickedEvent(version: String): Boolean {
        // ad_clicked event was added in 8.3.0
        val majorVersion = Util.extractMajorVersion(version)
        val minorVersion = Util.extractMinorVersion(version)

        if (majorVersion >= 9L) {
            return true
        }

        if (majorVersion == 8L && minorVersion >= 3L) {
            return true
        }

        return false
    }

    companion object {
        private const val TAG = "TheoPlayerAdAdapter"
    }
}
