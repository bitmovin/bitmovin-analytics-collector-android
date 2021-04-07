package com.bitmovin.analytics.bitmovin.player.features

import com.bitmovin.analytics.Observable
import com.bitmovin.analytics.ObservableSupport
import com.bitmovin.analytics.OnAnalyticsReleasingEventListener
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.deficiency.PlayerErrorCode
import com.bitmovin.player.api.deficiency.SourceErrorCode
import com.bitmovin.player.api.event.Event
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import java.lang.Exception
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
        var capturedPlayerEventErrorHandler: ((PlayerEvent.Error) -> Unit)? = null
        var capturedSourceEventErrorHandler: ((SourceEvent.Error) -> Unit)? = null
        every { player.on(any<KClass<Event>>(), any()) } answers {
            val firstArgument = it.invocation.args[0]

            if (firstArgument == PlayerEvent.Error::class) capturedPlayerEventErrorHandler = secondArg()
            if (firstArgument == SourceEvent.Error::class) capturedSourceEventErrorHandler = secondArg()
        }
        val adapter = BitmovinErrorDetailsAdapter(player, mockk(relaxed = true))

        // act
        val adapterSubscribeListener = mockk<OnErrorDetailEventListener>(relaxed = true)
        adapter.subscribe(adapterSubscribeListener)

        val playerEventError = PlayerEvent.Error(
                PlayerErrorCode.General,
                "test-player-event-error-message",
                Exception("test-player-event-error-exception")
        )
        playerEventError.timestamp = 12345
        capturedPlayerEventErrorHandler?.let { it(playerEventError) }

        val sourceEventError = SourceEvent.Error(
                SourceErrorCode.DrmGeneral,
                "test-source-event-error-message",
                Exception("test-source-event-error-exception")
        )
        sourceEventError.timestamp = 54321
        capturedSourceEventErrorHandler?.let { it(sourceEventError) }

        // assert
        verify { adapterSubscribeListener.onError(playerEventError.timestamp, playerEventError.code.value, playerEventError.message, playerEventError.data as? Throwable) }
        verify { adapterSubscribeListener.onError(sourceEventError.timestamp, sourceEventError.code.value, sourceEventError.message, sourceEventError.data as? Throwable) }
    }
}
