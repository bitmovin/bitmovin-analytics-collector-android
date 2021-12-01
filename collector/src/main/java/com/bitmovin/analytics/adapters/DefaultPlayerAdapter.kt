package com.bitmovin.analytics.adapters

import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

abstract class DefaultPlayerAdapter(private val eventDataFactory: EventDataFactory, private val playerStateMachine: PlayerStateMachine, private val deviceInformationProvider: DeviceInformationProvider) : PlayerAdapter {

    override fun createEventData() =
        eventDataFactory.create(
                playerStateMachine.impressionId,
                currentSourceMetadata,
                deviceInformationProvider.getDeviceInformation())
}
