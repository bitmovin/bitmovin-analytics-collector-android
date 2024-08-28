package com.bitmovin.analytics.ssai

import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdPosition
import com.bitmovin.analytics.api.ssai.SsaiAdQuartile
import com.bitmovin.analytics.api.ssai.SsaiAdQuartileMetadata
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.internal.InternalBitmovinApi
import com.bitmovin.analytics.utils.SystemTimeService
import com.bitmovin.analytics.utils.Util

@InternalBitmovinApi
class SsaiEngagementMetricsService(
    private val analytics: BitmovinAnalytics,
    private val playerAdapter: PlayerAdapter,
    private val ssaiTimeoutHandler: Handler,
    private val systemTimeService: SystemTimeService = SystemTimeService(),
) {
    private var adImpressionId: String? = null
    private var quartilesFinishedWithCurrentAd = mutableSetOf<SsaiAdQuartile>()
    private var errorSentForCurrentAd = false
    private var activeAdSample: AdEventData? = null
    private var adStartedAtInMs: Long = 0

    @Synchronized
    fun markAdStart(
        adPosition: SsaiAdPosition?,
        adMetadata: SsaiAdMetadata?,
        adIndex: Int,
    ) {
        flushCurrentAdSample()
        resetStateOnNewAd()
        adImpressionId = Util.uUID
        val adEventData = createBasicSsaiAdEventData(adPosition, adMetadata, adIndex)
        adEventData.started = 1
        adStartedAtInMs = systemTimeService.elapsedRealtime()
        activeAdSample = adEventData
        enableOrPostponeFlushTimeout()
    }

    @Synchronized
    fun markQuartileFinished(
        adPosition: SsaiAdPosition?,
        quartile: SsaiAdQuartile,
        adMetadata: SsaiAdMetadata?,
        adQuartileMetadata: SsaiAdQuartileMetadata?,
        adIndex: Int,
    ) {
        // we make sure that each quartile is sent at most once per ad id,
        // to avoid duplicates in the metrics
        if (quartilesFinishedWithCurrentAd.add(quartile)) {
            createAndUpsertQuartileSample(adPosition, quartile, adMetadata, adQuartileMetadata, adIndex)

            // we only send the sample out if the ad is completed
            // partially watched ads are sent through
            // pausing of the app, starting a new ad, or ad break end
            if (quartile == SsaiAdQuartile.COMPLETED) {
                flushCurrentAdSample()
            } else {
                enableOrPostponeFlushTimeout()
            }
        }
    }

    @Synchronized
    fun sendAdErrorSample(
        adPosition: SsaiAdPosition?,
        adMetadata: SsaiAdMetadata?,
        adIndex: Int,
        errorCode: Int,
        errorMessage: String,
    ) {
        // we need to make sure that we only send one error sample per active ad, to not inflate metrics
        if (errorSentForCurrentAd) {
            return
        }
        errorSentForCurrentAd = true
        val adEventData = createBasicSsaiAdEventData(adPosition, adMetadata, adIndex)
        adEventData.errorMessage = errorMessage
        adEventData.errorCode = errorCode
        upsertAdSample(adEventData)
        flushCurrentAdSample()
    }

    @Synchronized
    fun flushCurrentAdSample() {
        sendAndClearAdSample()
    }

    private fun sendAndClearAdSample() {
        activeAdSample?.let {
            it.timeSinceAdStartedInMs = systemTimeService.elapsedRealtime() - adStartedAtInMs
            analytics.sendAdEventData(it)
        }
        activeAdSample = null
        disableFlushTimeout()
    }

    private fun createAndUpsertQuartileSample(
        adPosition: SsaiAdPosition?,
        adQuartile: SsaiAdQuartile,
        adMetadata: SsaiAdMetadata?,
        adQuartileMetadata: SsaiAdQuartileMetadata?,
        adIndex: Int,
    ) {
        val adEventData = createBasicSsaiAdEventData(adPosition, adMetadata, adIndex)
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
        }

        upsertAdSample(adEventData)
    }

    private fun createBasicSsaiAdEventData(
        adPosition: SsaiAdPosition?,
        adMetadata: SsaiAdMetadata?,
        adIndex: Int,
    ): AdEventData {
        val eventData = playerAdapter.createEventDataForAdSample()
        val adEventData = AdEventData.fromEventData(eventData, AdType.SERVER_SIDE)
        adEventData.adImpressionId = getOrCreateAdImpressionId()
        adEventData.adId = adMetadata?.adId
        adEventData.adSystem = adMetadata?.adSystem
        adEventData.adPosition = adPosition?.position
        adEventData.adIndex = adIndex
        return adEventData
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

    private fun mergeErrorInfo(
        adSample1: AdEventData,
        adSample2: AdEventData,
    ) {
        adSample1.errorCode =
            if (adSample1.errorCode != null) {
                adSample1.errorCode
            } else {
                adSample2.errorCode
            }

        adSample1.errorMessage =
            if (adSample1.errorMessage != null) {
                adSample1.errorMessage
            } else {
                adSample2.errorMessage
            }
    }

    private fun getOrCreateAdImpressionId(): String {
        return adImpressionId ?: Util.uUID
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
            flushCurrentAdSample()
        }, FLUSH_TIMEOUT_MS)
    }

    private fun disableFlushTimeout() {
        ssaiTimeoutHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val FLUSH_TIMEOUT_MS = 60000L
    }
}
