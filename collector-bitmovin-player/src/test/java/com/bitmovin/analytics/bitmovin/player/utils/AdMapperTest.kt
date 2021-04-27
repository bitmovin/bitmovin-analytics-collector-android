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
    fun FromPlayerAdWithAdDataNullShouldNotSetAdDataValues() {
        `when`(playerAd.data).thenReturn(null)
        val collectorAd = adMapper.fromPlayerAd(playerAd)

        assertThat(collectorAd.bitrate).isNull()
        assertThat(collectorAd.title).isNull()
    }

    @Test
    fun FromPlayerAdWithLinearAdShouldSetLinearAdValues() {
        val TEN_SENCONDS = 10.0
        val TEN_SECONDS_IN_MS = TEN_SENCONDS.toLong() * 1000

        `when`(linearAd.duration).thenReturn(TEN_SENCONDS)
        val collectorAd = adMapper.fromPlayerAd(linearAd)

        assertThat(collectorAd.duration).isEqualTo(TEN_SECONDS_IN_MS)
    }

    @Test
    fun FromPlayerAdWithVastAdDataShouldSetVastAdDataValues() {
        val TITLE = "Title"
        `when`(playerAd.data).thenReturn(vastAdData)
        `when`((playerAd.data as VastAdData).adTitle).thenReturn(TITLE)
        val collectorAd = adMapper.fromPlayerAd(playerAd)

        assertThat(collectorAd.title).isEqualTo(TITLE)
    }

    @Test
    fun FromPlayerAdWithImaAdDataShouldSetImaAdDataValues() {
        val DEALID = "dealid"
        `when`(playerAd.data).thenReturn(imaAdData)
        `when`((playerAd.data as ImaAdData).dealId).thenReturn(DEALID)
        val collectorAd = adMapper.fromPlayerAd(playerAd)

        assertThat(collectorAd.dealId).isEqualTo(DEALID)
    }
}
