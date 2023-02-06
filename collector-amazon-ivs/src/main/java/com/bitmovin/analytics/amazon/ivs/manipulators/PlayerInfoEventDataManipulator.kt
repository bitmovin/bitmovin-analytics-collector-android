package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator

/**
 * Manipulator for static player info
 * Fields handled:
 *  - version
 */
internal class PlayerInfoEventDataManipulator(private val player: Player) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        data.version = player.version
    }
}
