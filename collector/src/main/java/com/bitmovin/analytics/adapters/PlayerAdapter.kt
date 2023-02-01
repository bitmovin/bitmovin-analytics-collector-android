package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

interface PlayerAdapter {
    val stateMachine: PlayerStateMachine
    val position: Long
    val drmDownloadTime: Long?
    val currentSourceMetadata: SourceMetadata?
    val playerInfo: PlayerInfo

    fun init(): Collection<Feature<FeatureConfigContainer, *>>
    fun release()
    fun resetSourceRelatedState()
    fun clearValues()
    fun createEventData(): EventData
    fun createAdAdapter(): AdAdapter? {
        return null
    }
}
