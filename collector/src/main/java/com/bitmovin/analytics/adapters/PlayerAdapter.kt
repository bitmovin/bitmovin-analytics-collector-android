package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

interface PlayerAdapter {
    val stateMachine: PlayerStateMachine
    val position: Long
    val drmDownloadTime: Long?
    var defaultMetadata: DefaultMetadata
    val playerInfo: PlayerInfo

    fun init(): Collection<Feature<FeatureConfigContainer, *>>
    fun release()
    fun resetSourceRelatedState()

    // Called after sending EventData
    // Should clear all values that are only needed per sample
    fun clearValuesAfterSendingOfSample()
    fun createEventData(): EventData
    fun createEventDataForCustomDataEvent(sourceMetadata: SourceMetadata): EventData

    fun createAdAdapter(): AdAdapter? {
        return null
    }

    fun getCurrentSourceMetadata(): SourceMetadata
}
