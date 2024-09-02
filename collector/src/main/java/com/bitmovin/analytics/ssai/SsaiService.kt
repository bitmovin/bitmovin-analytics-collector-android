package com.bitmovin.analytics.ssai

import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdQuartile
import com.bitmovin.analytics.api.ssai.SsaiAdQuartileMetadata
import com.bitmovin.analytics.api.ssai.SsaiApi
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

private enum class SsaiState {
    AD_BREAK_STARTED, // Ad Break Started but no ssai ad is running yet
    AD_RUNNING, // SSAI Ad is running within ad break
    NOT_ACTIVE, // Normal playback outside of ad break
}

const val INDEX_INITIAL_VALUE = -1

class SsaiService(
    private val stateMachine: PlayerStateMachine,
    private val ssaiEngagementMetricsService: SsaiEngagementMetricsService,
) : SsaiApi, EventDataManipulator {
    var adMetadata: SsaiAdMetadata? = null
        private set
    private var adBreakMetadata: SsaiAdBreakMetadata? = null
    private var isFirstSampleOfAd = false
    private var state: SsaiState = SsaiState.NOT_ACTIVE

    // we start with -1, to be able to increment it before sending the first sample
    // this way the current adIndex is the one of the current playing ad
    private var adIndex: Int = INDEX_INITIAL_VALUE

    override fun adBreakStart(adBreakMetadata: SsaiAdBreakMetadata?) {
        if (this.state != SsaiState.NOT_ACTIVE) {
            return
        }

        this.state = SsaiState.AD_BREAK_STARTED
        this.adBreakMetadata = adBreakMetadata
    }

    override fun adStart(adMetadata: SsaiAdMetadata?) {
        if (this.state == SsaiState.NOT_ACTIVE) {
            return
        }
        this.ssaiEngagementMetricsService.flushCurrentAdSample()

        stateMachine.triggerSampleIfPlaying(ssaiRelated = true)

        this.state = SsaiState.AD_RUNNING
        // to include the adIndex in the next sample which will be the first sample of this started ad
        this.isFirstSampleOfAd = true
        this.adIndex++
        this.adMetadata = adMetadata
        ssaiEngagementMetricsService.markAdStart(this.adBreakMetadata?.adPosition, adMetadata, this.adIndex)
    }

    override fun adBreakEnd() {
        if (this.state == SsaiState.NOT_ACTIVE) {
            return
        }

        this.ssaiEngagementMetricsService.flushCurrentAdSample()

        if (this.state == SsaiState.AD_RUNNING) {
            stateMachine.triggerSampleIfPlaying(ssaiRelated = true)
        }
        this.resetAdBreakRelatedState()
    }

    override fun adQuartileFinished(
        adQuartile: SsaiAdQuartile,
        adQuartileMetadata: SsaiAdQuartileMetadata?,
    ) {
        if (this.state != SsaiState.AD_RUNNING) {
            return
        }

        ssaiEngagementMetricsService.markQuartileFinished(
            this.adBreakMetadata?.adPosition,
            adQuartile,
            adMetadata,
            adQuartileMetadata,
            adIndex,
        )
    }

    fun flushCurrentAdSample() {
        ssaiEngagementMetricsService.flushCurrentAdSample()
    }

    fun sendAdErrorSample(
        errorCode: Int,
        errorMessage: String,
    ) {
        if (this.state != SsaiState.AD_RUNNING) {
            return
        }

        ssaiEngagementMetricsService.sendAdErrorSample(
            this.adBreakMetadata?.adPosition,
            adMetadata,
            adIndex,
            errorCode,
            errorMessage,
        )
    }

    fun resetSourceRelatedState() {
        this.resetAdBreakRelatedState()
        this.adIndex = INDEX_INITIAL_VALUE
    }

    private fun resetAdBreakRelatedState() {
        this.adMetadata = null
        this.adBreakMetadata = null
        this.isFirstSampleOfAd = false
        this.state = SsaiState.NOT_ACTIVE
    }

    override fun manipulate(data: EventData) {
        if (this.state != SsaiState.AD_RUNNING) {
            return
        }

        manipulateBasicData(data)

        // we only mark the first sample of an ssai ad with the index in the audience data (non ad table)
        // which allows us to count the number of ads
        if (this.isFirstSampleOfAd) {
            this.isFirstSampleOfAd = false
            data.adIndex = this.adIndex
        }
    }

    override fun manipulateForAdEvent(data: EventData) {
        if (this.state != SsaiState.AD_RUNNING) {
            return
        }

        manipulateBasicData(data)
        // we always want to include the adIndex in the ad event, since this helps us to map
        // the adSamples to the normal ssai eventData
        data.adIndex = this.adIndex
    }

    private fun manipulateBasicData(data: EventData) {
        data.ad = AdType.SERVER_SIDE.value
        data.adId = this.adMetadata?.adId
        data.adSystem = this.adMetadata?.adSystem
        data.adPosition = this.adBreakMetadata?.adPosition?.toString()
        data.ssaiRelatedSample = true
    }
}
