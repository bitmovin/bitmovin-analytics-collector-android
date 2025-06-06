package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.dtos.FeatureConfigContainer
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

interface PlayerAdapter {
    val stateMachine: PlayerStateMachine
    val position: Long
    val drmDownloadTime: Long?
    var defaultMetadata: DefaultMetadata
    val playerInfo: PlayerInfo
    val isAutoplayEnabled: Boolean?
    val ssaiService: SsaiService

    fun init(): Collection<Feature<FeatureConfigContainer, *>>

    fun release()

    fun resetSourceRelatedState()

    fun createEventData(): EventData

    fun createEventDataForAdSample(): EventData

    fun createEventDataForCustomDataEvent(sourceMetadata: SourceMetadata): EventData

    fun createAdAdapter(): AdAdapter? {
        return null
    }

    fun getCurrentSourceMetadata(): SourceMetadata

    fun triggerLastSampleOfSession()
}
