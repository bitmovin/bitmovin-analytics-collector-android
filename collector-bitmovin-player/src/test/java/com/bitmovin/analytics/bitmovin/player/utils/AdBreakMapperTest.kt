package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.AdPosition
import com.bitmovin.player.model.advertising.AdBreak
import com.bitmovin.player.model.advertising.AdConfiguration
import com.bitmovin.player.model.advertising.AdTag
import com.bitmovin.player.model.advertising.AdTagType
import com.bitmovin.player.model.advertising.ima.ImaAdBreak
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class AdBreakMapperTest {

    @Mock
    private lateinit var adConfiguration: AdConfiguration

    @Mock
    private lateinit var adBreak: AdBreak

    @Mock
    private lateinit var imaAdBreak: ImaAdBreak

    private var adBreakMapper = AdBreakMapper()


    @Before
    fun setup(){
        MockitoAnnotations.initMocks(this)
        `when`(adBreak.id).thenReturn("id")
        `when`(imaAdBreak.id).thenReturn("id")
        `when`(imaAdBreak.tag).thenReturn(AdTag("", AdTagType.VMAP))
    }

    @Test
    fun test__FromPlayerAdConifurationWithAdConfiguration(){
        `when`(adConfiguration.replaceContentDuration).thenReturn(10.0)
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(adConfiguration)

        Assert.assertNotNull(collectorAdBreak.replaceContentDuration)
        Assert.assertNull(collectorAdBreak.scheduleTime)
    }

    @Test
    fun test__FromPlayerAdConfigurationWithAdBreak() {
        `when`(adBreak.scheduleTime).thenReturn(10.0)
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(adBreak)

        Assert.assertNotNull(collectorAdBreak.scheduleTime)
        Assert.assertNull(collectorAdBreak.position)
    }

    @Test
    fun test__FromPlayerAdConfigurationWithImaAdBreak(){
        `when`(imaAdBreak.position).thenReturn("pre")
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(imaAdBreak)

        Assert.assertNotNull(collectorAdBreak.position)
        Assert.assertEquals(collectorAdBreak.position, AdPosition.pre)
    }
}