package com.bitmovin.analytics.utils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import java.lang.Exception
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UtilTest {

    @Test
    fun testTopOfStacktrace() {
        try {
            throw RuntimeException("RUNTIMEEXCEPTION")
        } catch (e: Exception) {
            val top = e.topOfStacktrace
            assertThat(top).hasSize(10)
            assertThat(top).anySatisfy { element -> assertThat(element).contains("testTopOfStacktrace") }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsConfigValueTrueIfPlayerNotReady() {
        val config = BitmovinAnalyticsConfig()
        config.setIsLive(true)

        val isLive = Util.getIsLiveFromConfigOrPlayer(false, config.isLive, false)
        assertThat(isLive).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsConfigValueFalseIfPlayerNotReady() {
        val config = BitmovinAnalyticsConfig()
        config.setIsLive(false)

        val isLive = Util.getIsLiveFromConfigOrPlayer(false, config.isLive, false)
        assertThat(isLive).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsPlayerIsLiveTrueIfPlayerReady() {
        val config = BitmovinAnalyticsConfig()
        config.setIsLive(true)

        val isLive = Util.getIsLiveFromConfigOrPlayer(true, config.isLive, true)
        assertThat(isLive).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsPlayerIsLiveFalseIfPlayerReady() {
        val config = BitmovinAnalyticsConfig()
        config.setIsLive(true)

        val isLive = Util.getIsLiveFromConfigOrPlayer(true, config.isLive, false)
        assertThat(isLive).isFalse()
    }

    @Test
    fun toPrimitiveLong() {
        assertThat(Util.toPrimitiveLong(-1.2)).isEqualTo(-1)
        assertThat(Util.toPrimitiveLong(-1.6)).isEqualTo(-1)
        assertThat(Util.toPrimitiveLong(0.0)).isEqualTo(0)
        assertThat(Util.toPrimitiveLong(1.2)).isEqualTo(1)
        assertThat(Util.toPrimitiveLong(1.6)).isEqualTo(1)
        assertThat(Util.toPrimitiveLong(null)).isEqualTo(0)
    }
}
