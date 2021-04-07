package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.deficiency.ErrorEvent
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.EventListener
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import kotlin.reflect.KClass
import org.junit.Test

class BitmovinErrorDetailsAdapterTests {
    @Test
    fun `init wires events`() {
        // arrange
        val analyticsReleasing = mockk<Observable<OnAnalyticsReleasingEventListener>>(relaxed = true)
        val player = mockk<Player>(relaxed = true)

        // act
        val adapter = BitmovinErrorDetailsAdapter(player, analyticsReleasing)

        // assert
        verify { analyticsReleasing.subscribe(adapter) }
        verify { player.on(PlayerEvent.Error::class, any()) }
        verify { player.on(SourceEvent.Error::class, any()) }
    }

    @Test
    fun `onReleasing is called, when observable notify adapter`() {
        // arrange
        val analyticsReleasing = ObservableSupport<OnAnalyticsReleasingEventListener>()
        val player = mockk<Player>(relaxed = true)
        val adapter = BitmovinErrorDetailsAdapter(player, analyticsReleasing)
        mockkObject(adapter)

        // act
        analyticsReleasing.notify { it.onReleasing() }

        // assert
        verify { adapter.onReleasing() }
        verify(exactly = 2) { player.off(any<(Event) -> Unit>()) }

        // act
        clearMocks(adapter)
        analyticsReleasing.notify { it.onReleasing() }
        // assert
        verify(exactly = 0) { adapter.onReleasing() }
    }

    @Test
    fun `subscribe adds listener and call it, when player event is triggered`() {
        // arrange

        val player = mockk<Player>(relaxed = true)

        val capturedPlayerEvent = slot<(PlayerEvent.Error) -> Unit>()
        val capturedSourceEvent = slot<(SourceEvent.Error) -> Unit>()
        every { player.on(any<KClass<PlayerEvent.Error>>(), capture(capturedPlayerEvent)) } answers {


        }
//        every { player.on(any(), capture(capturedSourceEvent)) } answers {}

        val adapter = BitmovinErrorDetailsAdapter(player, mockk(relaxed = true))

        // act
        val adapterSubscribeListener = mockk<OnErrorDetailEventListener>(relaxed = true)
        adapter.subscribe(adapterSubscribeListener)

        capturedPlayerEvent.captured(mockk(relaxed = true))
        capturedSourceEvent.captured(mockk(relaxed = true))

        // assert
        verify(exactly = 2) { adapterSubscribeListener.onError(any(), any(), any(), any()) }
    }
}
