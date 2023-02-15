package com.bitmovin.analytics.amazon.ivs.manipulators

import android.util.Log
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
        try {
            data.version = player.version
        } catch (e: Exception) {
            Log.e("PlayerInfoManipulator", "Something went wrong while setting player info event data, e: ${e.message}", e)
        }
    }
}
