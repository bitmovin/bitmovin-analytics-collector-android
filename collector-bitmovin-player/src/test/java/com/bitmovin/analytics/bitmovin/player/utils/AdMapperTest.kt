package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.player.model.advertising.Ad
import com.bitmovin.player.model.advertising.LinearAd
import com.bitmovin.player.model.advertising.VastAdData
import com.bitmovin.player.model.advertising.ima.ImaAdData
import junit.framework.Assert
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

    private var adMapper = AdMapper()

    @Before
    fun setup(){
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun FromPlayerAdWithAdDataNullShouldNotSetAdDataValues() {
        `when`(playerAd.data).thenReturn(null)
        var collectorAd = adMapper.FromPlayerAd(playerAd)

        assertThat(collectorAd.bitrate).isNull()
        assertThat(collectorAd.title).isNull()
    }

    @Test
    fun FromPlayerAdWithLinearAdShouldSetLinearAdValues() {
        val TEN_SENCONDS = 10.0
        val TEN_SECONDS_IN_MS = TEN_SENCONDS * 1000

        `when`(linearAd.duration).thenReturn(TEN_SENCONDS)
        var collectorAd = adMapper.FromPlayerAd(linearAd)

        assertThat(collectorAd.duration).isEqualTo(TEN_SECONDS_IN_MS)
    }

    @Test
    fun FromPlayerAdWithVastAdDataShouldSetVastAdDataValues(){
        val TITLE = "Title"
        `when`(playerAd.data).thenReturn(vastAdData)
        `when`((playerAd.data as VastAdData).adTitle).thenReturn(TITLE)
        var collectorAd = adMapper.FromPlayerAd(playerAd)

        assertThat(collectorAd.title).isEqualTo(TITLE)
    }

    @Test
    fun FromPlayerAdWithImaAdDataShouldSetImaAdDataValues(){
        val DEALID = "dealid"
        `when`(playerAd.data).thenReturn(imaAdData)
        `when`((playerAd.data as ImaAdData).dealId).thenReturn(DEALID)
        var collectorAd = adMapper.FromPlayerAd(playerAd)

        assertThat(collectorAd.dealId).isEqualTo(DEALID)
    }
}