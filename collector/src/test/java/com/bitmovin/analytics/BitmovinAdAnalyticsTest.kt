package com.bitmovin.analytics

import com.bitmovin.analytics.adapters.AdAdapter
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.ads.Ad
import com.bitmovin.analytics.ads.AdBreak
import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.utils.Util
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class BitmovinAdAnalyticsTest {
    private val analytics: BitmovinAnalytics = mockk(relaxed = true)
    private val playerAdapter: PlayerAdapter = mockk(relaxed = true)
    private val adAdapter: AdAdapter = mockk(relaxed = true)
    private val sentSamples = mutableListOf<AdEventData>()
    private var now = 0L

    private lateinit var adAnalytics: BitmovinAdAnalytics

    @Before
    fun setup() {
        mockkObject(Util)
        every { Util.elapsedTime } answers { now }
        every { playerAdapter.stateMachine.isStartupFinished } returns false
        every { playerAdapter.createEventDataForAdSample() } answers { TestFactory.createEventData() }
        every { analytics.sendAdEventData(capture(sentSamples)) } returns Unit

        adAnalytics = BitmovinAdAnalytics(analytics)
        adAnalytics.attachAdapter(playerAdapter, adAdapter)
    }

    @After
    fun tearDown() {
        unmockkObject(Util)
    }

    @Test
    fun `pre-roll pod measures the first ad from PLAY and later ads from the previous ad's end`() {
        val firstAd = Ad(isLinear = true, id = "ad-1", duration = 15_000)
        val secondAd = Ad(isLinear = true, id = "ad-2", duration = 15_000)
        val adBreak = AdBreak(id = "pre-roll", ads = listOf(firstAd, secondAd))

        now = 1_000
        adAnalytics.onPlayEvent()
        now = 1_500
        adAnalytics.onAdBreakStarted(adBreak)
        now = 3_000
        adAnalytics.onAdStarted(firstAd)
        now = 18_000
        adAnalytics.onAdFinished()
        now = 18_100
        adAnalytics.onAdStarted(secondAd)
        now = 33_100
        adAnalytics.onAdFinished()

        verify(exactly = 2) { analytics.sendAdEventData(any()) }
        assertThat(sentSamples.map { it.adPodPosition to it.adStartupTime })
            .containsExactly(0 to 2_000L, 1 to 100L)
    }

    @Test
    fun `mid-roll pod measures the first ad from the break start and later ads from the previous ad's end`() {
        every { playerAdapter.stateMachine.isStartupFinished } returns true
        val firstAd = Ad(isLinear = true, id = "ad-1", duration = 15_000)
        val secondAd = Ad(isLinear = true, id = "ad-2", duration = 15_000)
        val adBreak = AdBreak(id = "mid-roll", ads = listOf(firstAd, secondAd))

        now = 100_000
        adAnalytics.onAdBreakStarted(adBreak)
        now = 100_400
        adAnalytics.onAdStarted(firstAd)
        now = 115_400
        adAnalytics.onAdFinished()
        now = 115_650
        adAnalytics.onAdStarted(secondAd)
        now = 130_650
        adAnalytics.onAdFinished()
        adAnalytics.onAdBreakFinished()

        verify(exactly = 2) { analytics.sendAdEventData(any()) }
        assertThat(sentSamples.map { it.adPodPosition to it.adStartupTime })
            .containsExactly(0 to 400L, 1 to 250L)
    }
}
