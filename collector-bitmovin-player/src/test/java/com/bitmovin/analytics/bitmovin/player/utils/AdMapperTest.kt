package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.player.model.advertising.Ad
import com.bitmovin.player.model.advertising.LinearAd
import com.bitmovin.player.model.advertising.VastAdData
import com.bitmovin.player.model.advertising.ima.ImaAdData
import junit.framework.Assert
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
    fun test__FromPlayerAdWithAdDataNull() {
        `when`(playerAd.data).thenReturn(null)
        var collectorAd = adMapper.FromPlayerAd(playerAd)

        Assert.assertNull(collectorAd.bitrate)
        Assert.assertNull(collectorAd.title)
    }

    @Test
    fun test__FromPlayerAdWithLinearAd() {
        `when`(linearAd.duration).thenReturn(10.0)
        var collectorAd = adMapper.FromPlayerAd(linearAd)

        Assert.assertEquals(collectorAd.duration, 10.0.toLong().times(1000))
    }

    @Test
    fun test__FromPlayerAdWithVastAdData(){
        `when`(playerAd.data).thenReturn(vastAdData)
        `when`((playerAd.data as VastAdData).adTitle).thenReturn("title")
        var collectorAd = adMapper.FromPlayerAd(playerAd)

        Assert.assertNotNull(collectorAd.title)
    }

    @Test
    fun test__FromPlayerAdWithImaAdData(){
        `when`(playerAd.data).thenReturn(imaAdData)
        `when`((playerAd.data as ImaAdData).dealId).thenReturn("dealid")
        var collectorAd = adMapper.FromPlayerAd(playerAd)

        Assert.assertNotNull(collectorAd.dealId)
    }


}