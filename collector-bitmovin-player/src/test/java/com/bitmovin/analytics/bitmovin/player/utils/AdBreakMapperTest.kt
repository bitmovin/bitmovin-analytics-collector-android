package com.bitmovin.analytics.bitmovin.player.utils

import com.bitmovin.analytics.ads.AdPosition
import com.bitmovin.player.model.advertising.AdBreak
import com.bitmovin.player.model.advertising.AdConfiguration
import com.bitmovin.player.model.advertising.AdTag
import com.bitmovin.player.model.advertising.AdTagType
import com.bitmovin.player.model.advertising.ima.ImaAdBreak
import junit.framework.Assert
import org.assertj.core.api.Assertions.assertThat
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
        `when`(adConfiguration.replaceContentDuration).thenReturn(10.0)

        `when`(adBreak.id).thenReturn("id")
        `when`(adBreak.scheduleTime).thenReturn(10.0)

        `when`(imaAdBreak.id).thenReturn("id")
        `when`(imaAdBreak.tag).thenReturn(AdTag("", AdTagType.VMAP))
        `when`(imaAdBreak.position).thenReturn("pre")
    }

    @Test
    fun FromPlayerAdConfigurationWithAdConfigurationShouldSetValuesOfAdConfiguration(){
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(adConfiguration)

        assertThat(collectorAdBreak.replaceContentDuration).isNotNull()
    }

    @Test
    fun FromPlayerAdConfigurationWithAdConfigurationShouldNotSetValuesOfAdBreak(){
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(adConfiguration)

        assertThat(collectorAdBreak.scheduleTime).isNull()
    }

    @Test
    fun FromPlayerAdConfigurationWithAdBreakShouldSetValuesOfAdBreak() {
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(adBreak)

        assertThat(collectorAdBreak.scheduleTime).isNotNull()
    }

    @Test
    fun FromPlayerAdConfigurationWithAdBreakShouldNotSetValuesOfImaAdBreak() {
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(adBreak)

        assertThat(collectorAdBreak.position).isNull()
    }

    @Test
    fun FromPlayerAdConfigurationWithImaAdBreakShouldSetValuesOfImaAdBreak(){
        `when`(imaAdBreak.currentFallbackIndex).thenReturn(0)
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(imaAdBreak)

        assertThat(collectorAdBreak.fallbackIndex).isNotNull()
    }
}