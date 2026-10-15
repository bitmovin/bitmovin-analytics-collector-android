package com.bitmovin.analytics.theoplayer

import com.theoplayer.android.api.event.EventListener
import com.theoplayer.android.api.event.EventType
import com.theoplayer.android.api.event.player.PlayerEvent
import com.theoplayer.android.api.event.player.PlayerEventTypes
import com.theoplayer.android.api.player.Player

// Reproduces AN-5942, where THEOplayer on NFL Android devices sometimes sends no PLAYING event after a play or seek
class PlayerWithSuppressedPlayingEvents(
    private val delegate: Player,
) : Player by delegate {
    @Volatile
    var suppressPlaying = true

    private val wrappedListeners = mutableMapOf<EventListener<*>, EventListener<PlayerEvent<*>>>()

    override fun <T : PlayerEvent<*>> addEventListener(
        type: EventType<T>,
        listener: EventListener<in T>,
    ) {
        if (type != PlayerEventTypes.PLAYING) {
            delegate.addEventListener(type, listener)
            return
        }
        @Suppress("UNCHECKED_CAST")
        val wrapped =
            EventListener<PlayerEvent<*>> { event ->
                if (!suppressPlaying) (listener as EventListener<PlayerEvent<*>>).handleEvent(event)
            }
        wrappedListeners[listener] = wrapped
        @Suppress("UNCHECKED_CAST")
        delegate.addEventListener(type as EventType<PlayerEvent<*>>, wrapped)
    }

    override fun <T : PlayerEvent<*>> removeEventListener(
        type: EventType<T>,
        listener: EventListener<in T>,
    ) {
        val wrapped = wrappedListeners.remove(listener)
        if (type != PlayerEventTypes.PLAYING || wrapped == null) {
            delegate.removeEventListener(type, listener)
            return
        }
        @Suppress("UNCHECKED_CAST")
        delegate.removeEventListener(type as EventType<PlayerEvent<*>>, wrapped)
    }
}
