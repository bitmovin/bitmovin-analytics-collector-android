package com.bitmovin.analytics.amazon.ivs.manipulators

import com.amazonaws.ivs.player.Player
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.utils.BitmovinLog

/**
 * Manipulator for static player info
 * Fields handled:
 *  - version
 */
internal class PlayerInfoEventDataManipulator(private val player: Player) : EventDataManipulator {
    override fun manipulate(data: EventData) {
        try {
            data.version = PlayerType.AMAZON_IVS.toString() + "-" + player.version
        } catch (e: Exception) {
            BitmovinLog.e(TAG, "Something went wrong while setting player info event data, e: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "PlayerInfoEventDataManipulator"
    }
}
