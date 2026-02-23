package com.bitmovin.analytics.amazon.ivs.manipulators

import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.dtos.EventData

/**
 * Manipulator for static player info
 * Fields handled:
 *  - version
 */
internal class PlayerInfoEventDataManipulator(private val playerContext: PlayerContext) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        data.version = playerContext.playerVersion
    }
}
