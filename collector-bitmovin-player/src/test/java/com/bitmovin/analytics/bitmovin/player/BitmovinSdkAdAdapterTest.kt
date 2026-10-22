package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.analytics.adapters.AdAnalyticsEventListener
import com.bitmovin.analytics.ads.AdPosition
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.advertising.AdBreak
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdSource
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.event.PlayerEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class BitmovinSdkAdAdapterTest {
    private val player: Player = mockk(relaxed = true)
    private val listener: AdAnalyticsEventListener = mockk(relaxed = true)
    private val adErrorListener = slot<(PlayerEvent.AdError) -> Unit>()

    private val failedAdItem =
        AdItem(
            "pre",
            AdSource(AdSourceType.Bitmovin, "https://ads.example.com/first.xml"),
            AdSource(AdSourceType.Bitmovin, "https://ads.example.com/fallback.xml"),
        )

    @Before
    fun setup() {
        every { player.on(PlayerEvent.AdError::class, capture(adErrorListener)) } answers { }
        BitmovinSdkAdAdapter(player).subscribe(listener)
    }

    @Test
    fun `ad that fails to load is reported as a load error of its last tried source`() {
        adErrorListener.captured(PlayerEvent.AdError(failedAdItem, 303, "No ads", null))

        verify(exactly = 1) {
            listener.onAdLoadError(
                match {
                    it.tagUrl == "https://ads.example.com/fallback.xml" &&
                        it.fallbackIndex == 1L &&
                        it.position == AdPosition.PRE &&
                        it.ads.isEmpty()
                },
                match { it.adModule == "DefaultAdvertisingService" },
                303,
                "No ads",
            )
        }
        verify(exactly = 0) { listener.onAdError(any(), any(), any()) }
    }

    @Test
    fun `load failure followed by the player's ad break not loaded error is reported once`() {
        adErrorListener.captured(PlayerEvent.AdError(failedAdItem, 303, "No ads", null))
        adErrorListener.captured(PlayerEvent.AdError(null, 2001, "Cannot play ad: Ad break not loaded", null))

        verify(exactly = 1) { listener.onAdLoadError(any(), any(), any(), any()) }
        verify(exactly = 0) { listener.onAdError(any(), any(), any()) }
    }

    @Test
    fun `error inside a started break is reported as an ad error`() {
        val adBreak: AdBreak = mockk(relaxed = true)
        every { adBreak.id } returns "break-1"

        adErrorListener.captured(PlayerEvent.AdError(failedAdItem, 2203, "Bad HTTP status", adBreak))

        verify(exactly = 1) { listener.onAdError(match { it.id == "break-1" }, 2203, "Bad HTTP status") }
        verify(exactly = 0) { listener.onAdLoadError(any(), any(), any(), any()) }
    }
}
