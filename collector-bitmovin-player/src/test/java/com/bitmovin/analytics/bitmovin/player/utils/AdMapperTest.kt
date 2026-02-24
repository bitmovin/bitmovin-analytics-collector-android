@file:Suppress("DEPRECATION")

package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.player.api.advertising.Ad
import com.bitmovin.player.api.advertising.LinearAd
import com.bitmovin.player.api.advertising.ima.ImaAdData
import com.bitmovin.player.api.advertising.vast.VastAdData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class AdMapperTest {
    @Mock
    private lateinit var playerAd: Ad

    @Mock
    private lateinit var linearAd: LinearAd

    @Mock
    private lateinit var vastAdData: VastAdData

    @Mock
    private lateinit var imaAdData: ImaAdData

    private val adMapper = AdMapper()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun fromPlayerAdWithAdDataNullShouldNotSetAdDataValues() {
        `when`(playerAd.data).thenReturn(null)
        val collectorAd = adMapper.fromPlayerAd(playerAd)

        assertThat(collectorAd.bitrate).isNull()
        assertThat(collectorAd.title).isNull()
    }

    @Test
    fun fromPlayerAdWithLinearAdShouldSetLinearAdValues() {
        `when`(linearAd.duration).thenReturn(TEN_SECONDS)
        val collectorAd = adMapper.fromPlayerAd(linearAd)

        assertThat(collectorAd.duration).isEqualTo(TEN_SECONDS_IN_MS)
    }

    @Test
    fun fromPlayerAdWithVastAdDataShouldSetVastAdDataValues() {
        val expectedTitle = "Title"
        `when`(playerAd.data).thenReturn(vastAdData)
        `when`((playerAd.data as VastAdData).adTitle).thenReturn(expectedTitle)
        val collectorAd = adMapper.fromPlayerAd(playerAd)

        assertThat(collectorAd.title).isEqualTo(expectedTitle)
    }

    @Test
    fun fromPlayerAdWithImaAdDataShouldSetImaAdDataValues() {
        val expectedDealId = "dealid"
        `when`(playerAd.data).thenReturn(imaAdData)
        `when`((playerAd.data as ImaAdData).dealId).thenReturn(expectedDealId)
        val collectorAd = adMapper.fromPlayerAd(playerAd)

        assertThat(collectorAd.dealId).isEqualTo(expectedDealId)
    }

    companion object {
        const val TEN_SECONDS = 10.0
        const val TEN_SECONDS_IN_MS = TEN_SECONDS.toLong() * 1000
    }
}
