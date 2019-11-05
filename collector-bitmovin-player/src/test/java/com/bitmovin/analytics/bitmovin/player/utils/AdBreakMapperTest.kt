package com.bitmovin.analytics.bitmovin.player.utils

import android.util.Log
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

    private val TEN_SECONDS = 10.0
    private val TEN_SECONDS_IN_MS: Long = TEN_SECONDS.toLong() *  1000
    private val ID = "id"
    private val PRE = "pre"

    @Before
    fun setup(){
        MockitoAnnotations.initMocks(this)
        `when`(adConfiguration.replaceContentDuration).thenReturn(TEN_SECONDS)

        `when`(adBreak.id).thenReturn(ID )
        `when`(adBreak.scheduleTime).thenReturn(TEN_SECONDS)

        `when`(imaAdBreak.id).thenReturn(ID )
        `when`(imaAdBreak.tag).thenReturn(AdTag("", AdTagType.VMAP))
        `when`(imaAdBreak.position).thenReturn(PRE)
    }

    @Test
    fun FromPlayerAdConfigurationWithAdConfigurationShouldSetValuesOfAdConfiguration(){
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(adConfiguration)

        assertThat(collectorAdBreak.replaceContentDuration).isEqualTo(TEN_SECONDS_IN_MS)
    }

    @Test
    fun FromPlayerAdConfigurationWithAdConfigurationShouldNotSetValuesOfAdBreak(){
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(adConfiguration)

        assertThat(collectorAdBreak.scheduleTime).isNull()
    }

    @Test
    fun FromPlayerAdConfigurationWithAdBreakShouldSetValuesOfAdBreak() {
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(adBreak)

        assertThat(collectorAdBreak.scheduleTime).isEqualTo(TEN_SECONDS_IN_MS)
    }

    @Test
    fun FromPlayerAdConfigurationWithAdBreakShouldNotSetValuesOfImaAdBreak() {
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(adBreak)

        assertThat(collectorAdBreak.position).isNull()
    }

    @Test
    fun FromPlayerAdConfigurationWithImaAdBreakShouldSetValuesOfImaAdBreak(){
        `when`(imaAdBreak.currentFallbackIndex).thenReturn(1)
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(imaAdBreak)

        assertThat(collectorAdBreak.fallbackIndex).isEqualTo(1)
    }

    @Test
    fun FromPlayerAdConfigurationPlayerPositionPREShouldSetPREPlayerPosition(){
        `when`(imaAdBreak.position).thenReturn("pre")
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(imaAdBreak)

        assertThat(collectorAdBreak.position).isEqualTo(AdPosition.pre)
    }

    @Test
    fun FromPlayerAdConfigurationPlayerPositionPOSTShouldSetPOSTPlayerPosition(){
        `when`(imaAdBreak.position).thenReturn("post")
        val collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(imaAdBreak)

        assertThat(collectorAdBreak.position).isEqualTo(AdPosition.post)
    }

    @Test
    fun FromPlayerAdConfigurationPlayerPositionMIDShouldSetMIDPlayerPosition(){
        `when`(imaAdBreak.position).thenReturn("10")
        var collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(imaAdBreak)
        assertThat(collectorAdBreak.position).isEqualTo(AdPosition.mid)

        `when`(imaAdBreak.position).thenReturn("25%")
        collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(imaAdBreak)
        assertThat(collectorAdBreak.position).isEqualTo(AdPosition.mid)

        `when`(imaAdBreak.position).thenReturn("00:10:00.000")
        collectorAdBreak = adBreakMapper.FromPlayerAdConfiguration(imaAdBreak)
        assertThat(collectorAdBreak.position).isEqualTo(AdPosition.mid)
    }
}