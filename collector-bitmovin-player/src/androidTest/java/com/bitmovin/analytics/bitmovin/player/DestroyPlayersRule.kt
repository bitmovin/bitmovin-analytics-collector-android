package com.bitmovin.analytics.bitmovin.player

import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.player.api.Player
import org.junit.rules.ExternalResource

// AN-5980: all system tests share one process, so a player left alive by a test keeps its buffers until the process runs out of memory
class DestroyPlayersRule : ExternalResource() {
    private val players = mutableListOf<Player>()

    fun <T : Player> track(player: T): T {
        synchronized(players) { players.add(player) }
        return player
    }

    override fun after() {
        val tracked = synchronized(players) { players.toList().also { players.clear() } }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            tracked.filterNot { it.isDestroyed }.forEach { it.destroy() }
        }
    }
}
