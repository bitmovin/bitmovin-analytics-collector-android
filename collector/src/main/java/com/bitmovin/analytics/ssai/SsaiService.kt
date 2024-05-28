package com.bitmovin.analytics.ssai

import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiApi
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

private enum class SsaiState {
    AD_BREAK_STARTED,
    ACTIVE,
    IDLE,
}

class SsaiService(
    private val stateMachine: PlayerStateMachine,
) : SsaiApi, EventDataManipulator {
    var adMetadata: SsaiAdMetadata? = null
        private set
    private var adBreakMetadata: SsaiAdBreakMetadata? = null
    private var isFirstSampleOfAd = false
    private var state: SsaiState = SsaiState.IDLE
    private var adIndex = 0

    override fun adBreakStart(adBreakMetadata: SsaiAdBreakMetadata?) {
        if (this.state != SsaiState.IDLE) {
            return
        }

        this.state = SsaiState.AD_BREAK_STARTED
        this.adBreakMetadata = adBreakMetadata
    }

    override fun adStart(adMetadata: SsaiAdMetadata?) {
        if (this.state == SsaiState.IDLE) {
            return
        }

        stateMachine.onPlayingHeartbeat()

        this.state = SsaiState.ACTIVE
        // to include the adIndex in the next sample which will be the first sample of this started ad
        this.isFirstSampleOfAd = true

        this.adMetadata = adMetadata
    }

    override fun adBreakEnd() {
        if (this.state == SsaiState.IDLE) {
            return
        }

        if (this.state == SsaiState.ACTIVE) {
            stateMachine.onPlayingHeartbeat()
        }

        this.resetAdBreakRelatedState()
    }

    private fun resetAdBreakRelatedState() {
        this.adMetadata = null
        this.adBreakMetadata = null
        this.isFirstSampleOfAd = false
        this.state = SsaiState.IDLE
    }

    fun resetSourceRelatedState() {
        this.resetAdBreakRelatedState()
        this.adIndex = 0
    }

    override fun manipulate(data: EventData) {
        if (this.state != SsaiState.ACTIVE) {
            return
        }

        data.ad = AdType.SERVER_SIDE.value
        data.adId = this.adMetadata?.adId
        data.adSystem = this.adMetadata?.adSystem
        if (this.adBreakMetadata != null && this.adBreakMetadata?.adPosition != null) {
            data.adPosition = this.adBreakMetadata?.adPosition.toString()
        }

        if (this.isFirstSampleOfAd) {
            data.adIndex = this.adIndex
            this.isFirstSampleOfAd = false
            adIndex++
        }
    }
}
