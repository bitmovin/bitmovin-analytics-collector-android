package com.bitmovin.analytics.ssai

import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.error.ErrorSeverity
import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdQuartile
import com.bitmovin.analytics.api.ssai.SsaiAdQuartileMetadata
import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.internal.InternalBitmovinApi
import com.bitmovin.analytics.utils.BitmovinLog
import com.bitmovin.analytics.utils.SystemTimeService
import com.bitmovin.analytics.utils.Util
import kotlin.time.DurationUnit
import kotlin.time.toKotlinDuration

@InternalBitmovinApi
class SsaiEngagementMetricsService(
    private val analytics: BitmovinAnalytics,
    private val analyticsConfig: AnalyticsConfig,
    private val playerAdapter: PlayerAdapter,
    private val ssaiTimeoutHandler: Handler,
    private val systemTimeService: SystemTimeService = SystemTimeService(),
) {
    private var adImpressionId: String? = null
    private var quartilesFinishedWithCurrentAd = mutableSetOf<SsaiAdQuartile>()
    private var errorSentForCurrentAd = false
    private var activeAdSample: AdEventData? = null
    private var adStartedAtInMs: Long = 0
    private var adPodPosition: Int = AD_POD_POSITION_INITIAL_VALUE

    fun adBreakStart() {
        this.adPodPosition = AD_POD_POSITION_INITIAL_VALUE
    }

    private var completedPaidAds: Int = 0
    private var completedSlates: Int = 0
    private var currentAdIsSlate: Boolean = false

    @Synchronized
    fun markAdStart(
        adBreakMetadata: SsaiAdBreakMetadata?,
        adMetadata: SsaiAdMetadata?,
        adIndex: Int,
    ) {
        if (!analyticsConfig.ssaiEngagementTrackingEnabled) {
            return
        }

        flushCurrentActiveAd(false)
        resetStateOnNewAd()
        currentAdIsSlate = adMetadata?.isSlate ?: false
        adImpressionId = Util.uUID
        this.adPodPosition++
        val adEventData = createBasicSsaiAdEventData(adBreakMetadata, adMetadata, adIndex)
        adEventData.started = 1
        adStartedAtInMs = systemTimeService.elapsedRealtime()
        activeAdSample = adEventData
        enableOrPostponeFlushTimeout()
    }

    @Synchronized
    fun markQuartileFinished(
        adBreakMetadata: SsaiAdBreakMetadata?,
        quartile: SsaiAdQuartile,
        adMetadata: SsaiAdMetadata?,
        adQuartileMetadata: SsaiAdQuartileMetadata?,
        adIndex: Int,
    ) {
        if (!analyticsConfig.ssaiEngagementTrackingEnabled) {
            return
        }

        // we make sure that each quartile is sent at most once per ad id,
        // to avoid duplicates in the metrics
        if (quartilesFinishedWithCurrentAd.add(quartile)) {
            if (quartile == SsaiAdQuartile.COMPLETED) {
                incrementCompletedCountForCurrentAd()
            }

            createAndUpsertQuartileSample(adBreakMetadata, quartile, adMetadata, adQuartileMetadata, adIndex)
            // We intentionally do not flush on COMPLETED here: we can't tell yet whether
            // this is the last sample of the ad break. The sample is flushed via the next
            // ad start, ad break end, error, or the flush timeout.
            enableOrPostponeFlushTimeout()
        }
    }

    @Synchronized
    fun sendAdErrorSample(
        adBreakMetadata: SsaiAdBreakMetadata?,
        adMetadata: SsaiAdMetadata?,
        adIndex: Int,
        errorCode: Int,
        errorMessage: String,
        errorSeverity: ErrorSeverity,
    ) {
        if (!analyticsConfig.ssaiEngagementTrackingEnabled) {
            return
        }

        // we need to make sure that we only send one error sample per active ad, to not inflate metrics
        if (errorSentForCurrentAd) {
            return
        }
        errorSentForCurrentAd = true
        val adEventData = createBasicSsaiAdEventData(adBreakMetadata, adMetadata, adIndex)
        adEventData.errorMessage = errorMessage
        adEventData.errorCode = errorCode
        adEventData.errorSeverity = errorSeverity
        upsertAdSample(adEventData)
        flushCurrentActiveAd(isLastSampleOfAdBreak = false)
    }

    @Synchronized
    fun flushCurrentActiveAd(isLastSampleOfAdBreak: Boolean) {
        if (!analyticsConfig.ssaiEngagementTrackingEnabled) {
            return
        }

        sendAndClearAdSample(isLastSampleOfAdBreak)
    }

    @Synchronized
    fun resetAdBreakState() {
        completedPaidAds = 0
        completedSlates = 0
        currentAdIsSlate = false
    }

    private fun sendAndClearAdSample(isLastSampleOfAdBreak: Boolean) {
        activeAdSample?.let {
            it.timeSinceAdStartedInMs = systemTimeService.elapsedRealtime() - adStartedAtInMs
            it.exitedAdBreak = isLastSampleOfAdBreak
            // Refresh the videoImpressionId at flush time so the sample is grouped with the
            // current impression. The sample was created at adStart and the impressionId may
            // have changed since (e.g. a player-driven source change reset the state machine);
            // without this, the ad sample would end up orphaned in a stale impression.
            it.videoImpressionId = analytics.impressionId
            analytics.sendAdEventData(it)
        }
        activeAdSample = null
        disableFlushTimeout()
    }

    private fun createAndUpsertQuartileSample(
        adBreakMetadata: SsaiAdBreakMetadata?,
        adQuartile: SsaiAdQuartile,
        adMetadata: SsaiAdMetadata?,
        adQuartileMetadata: SsaiAdQuartileMetadata?,
        adIndex: Int,
    ) {
        val adEventData = createBasicSsaiAdEventData(adBreakMetadata, adMetadata, adIndex)
        if (adQuartile == SsaiAdQuartile.FIRST) {
            adEventData.quartile1 = 1
            adEventData.quartile1FailedBeaconUrl = adQuartileMetadata?.failedBeaconUrl
        } else if (adQuartile == SsaiAdQuartile.MIDPOINT) {
            adEventData.midpoint = 1
            adEventData.midpointFailedBeaconUrl = adQuartileMetadata?.failedBeaconUrl
        } else if (adQuartile == SsaiAdQuartile.THIRD) {
            adEventData.quartile3 = 1
            adEventData.quartile3FailedBeaconUrl = adQuartileMetadata?.failedBeaconUrl
        } else if (adQuartile == SsaiAdQuartile.COMPLETED) {
            adEventData.completed = 1
            adEventData.completedFailedBeaconUrl = adQuartileMetadata?.failedBeaconUrl

            updateCompletedAdsInfo(adEventData, adBreakMetadata)
        }

        upsertAdSample(adEventData)
    }

    private fun createBasicSsaiAdEventData(
        adBreakMetadata: SsaiAdBreakMetadata?,
        adMetadata: SsaiAdMetadata?,
        adIndex: Int,
    ): AdEventData {
        val eventData = playerAdapter.createEventDataForAdSample()
        val adEventData = AdEventData.fromEventData(eventData, AdType.SERVER_SIDE)
        adEventData.adImpressionId = getOrCreateAdImpressionId()
        adEventData.adId = adMetadata?.adId
        adEventData.adSystem = adMetadata?.adSystem
        adEventData.adPosition = adBreakMetadata?.adPosition?.position
        adEventData.adIndex = adIndex
        adEventData.adPodPosition = this.adPodPosition
        adEventData.isSlate = adMetadata?.isSlate ?: false
        adEventData.adDuration = adMetadata?.duration?.toKotlinDuration()?.toLong(DurationUnit.MILLISECONDS)
        adEventData.expectedPaidAds = limitToPositiveValues(adBreakMetadata?.expectedPaidAds, "expectedPaidAds")
        adEventData.expectedSlates = limitToPositiveValues(adBreakMetadata?.expectedSlates, "expectedSlates")

        updateCompletedAdsInfo(adEventData, adBreakMetadata)
        return adEventData
    }

    private fun incrementCompletedCountForCurrentAd() {
        if (currentAdIsSlate) {
            completedSlates += 1
        } else {
            completedPaidAds += 1
        }
    }

    private fun updateCompletedAdsInfo(
        adEventData: AdEventData,
        adBreakMetadata: SsaiAdBreakMetadata?,
    ) {
        // Completion counts are only meaningful when the surrounding break declared expected values
        adEventData.completedPaidAds =
            if (adBreakMetadata?.expectedPaidAds != null) completedPaidAds else null
        adEventData.completedSlates =
            if (adBreakMetadata?.expectedSlates != null) completedSlates else null
    }

    private fun upsertAdSample(newAdSample: AdEventData) {
        // we temporary store the current ad sample in a local variable
        // to avoid issues where the current ad sample is updated while merging
        // this is also covered with synchronizing of the methods,
        // but it is an additional safety measure
        // where the type system guarantees that the current ad sample is never null
        val localAdSample = activeAdSample
        if (localAdSample == null) {
            this.activeAdSample = newAdSample
        } else {
            mergeQuartileInfo(localAdSample, newAdSample)
            mergeErrorInfo(localAdSample, newAdSample)
            mergeBreakCompletionInfo(localAdSample, newAdSample)
            this.activeAdSample = localAdSample
        }
    }

    private fun mergeQuartileInfo(
        adSample1: AdEventData,
        adSample2: AdEventData,
    ) {
        adSample1.started = if (adSample1.started == 1L) 1L else adSample2.started
        adSample1.quartile1 = if (adSample1.quartile1 == 1L) 1L else adSample2.quartile1
        adSample1.midpoint = if (adSample1.midpoint == 1L) 1L else adSample2.midpoint
        adSample1.quartile3 = if (adSample1.quartile3 == 1L) 1L else adSample2.quartile3
        adSample1.completed = if (adSample1.completed == 1L) 1L else adSample2.completed

        adSample1.quartile1FailedBeaconUrl =
            if (adSample1.quartile1FailedBeaconUrl != null) {
                adSample1.quartile1FailedBeaconUrl
            } else {
                adSample2.quartile1FailedBeaconUrl
            }

        adSample1.midpointFailedBeaconUrl =
            if (adSample1.midpointFailedBeaconUrl != null) {
                adSample1.midpointFailedBeaconUrl
            } else {
                adSample2.midpointFailedBeaconUrl
            }

        adSample1.quartile3FailedBeaconUrl =
            if (adSample1.quartile3FailedBeaconUrl != null) {
                adSample1.quartile3FailedBeaconUrl
            } else {
                adSample2.quartile3FailedBeaconUrl
            }

        adSample1.completedFailedBeaconUrl =
            if (adSample1.completedFailedBeaconUrl != null) {
                adSample1.completedFailedBeaconUrl
            } else {
                adSample2.completedFailedBeaconUrl
            }
    }

    // we make sure that when we merge the critical error wins
    // thus we first check if there is an critical error which is then prefered
    // if no critical error is present first error wins
    private fun mergeErrorInfo(
        adSample1: AdEventData,
        adSample2: AdEventData,
    ) {
        if (adSample1.errorCode != null && adSample1.errorSeverity == ErrorSeverity.CRITICAL) {
            // do nothing, critical error already present in adSample1
            return
        }

        // in case adSample1 has no critical error, but adSample2 has one, we take the error from adSample2
        if (adSample2.errorCode != null && adSample2.errorSeverity == ErrorSeverity.CRITICAL) {
            adSample1.errorCode = adSample2.errorCode
            adSample1.errorMessage = adSample2.errorMessage
            adSample1.errorSeverity = adSample2.errorSeverity
            return
        }

        if (adSample1.errorCode != null) {
            // do nothing, adSample1 already has an error, but INFO
            return
        }

        if (adSample2.errorCode != null) {
            adSample1.errorCode = adSample2.errorCode
            adSample1.errorMessage = adSample2.errorMessage
            adSample1.errorSeverity = adSample2.errorSeverity
            return
        }
    }

    // The counters live on the service and are recomputed for every new sample;
    // copy them over so the most recent (and highest) value wins after a merge.
    private fun mergeBreakCompletionInfo(
        adSample1: AdEventData,
        adSample2: AdEventData,
    ) {
        adSample1.completedPaidAds = adSample2.completedPaidAds ?: adSample1.completedPaidAds
        adSample1.completedSlates = adSample2.completedSlates ?: adSample1.completedSlates
    }

    private fun getOrCreateAdImpressionId(): String {
        return adImpressionId ?: Util.uUID
    }

    private fun limitToPositiveValues(
        number: Int?,
        fieldName: String,
    ): Int? {
        if (number == null) {
            return null
        }

        if (number < 0) {
            BitmovinLog.w(TAG, "$fieldName must not be negative, but was $number. Clamping it to 0.")
            return 0
        }

        return number
    }

    private fun resetStateOnNewAd() {
        errorSentForCurrentAd = false
        quartilesFinishedWithCurrentAd.clear()
    }

    private fun enableOrPostponeFlushTimeout() {
        disableFlushTimeout()
        enableFlushTimeout()
    }

    private fun enableFlushTimeout() {
        ssaiTimeoutHandler.postDelayed({
            flushCurrentActiveAd(false)
        }, FLUSH_TIMEOUT_MS)
    }

    private fun disableFlushTimeout() {
        ssaiTimeoutHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "SsaiEngagementMetrics"
        private const val FLUSH_TIMEOUT_MS = 60000L
        private const val AD_POD_POSITION_INITIAL_VALUE = -1
    }
}
