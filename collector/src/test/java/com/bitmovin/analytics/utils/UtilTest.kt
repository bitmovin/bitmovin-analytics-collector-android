package com.bitmovin.analytics.utils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import java.lang.Exception
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
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
        config.isLive = true

        val isLive = Util.getIsLiveFromConfigOrPlayer(false, config.isLive, false)
        assertThat(isLive).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsConfigValueFalseIfPlayerNotReady() {
        val config = BitmovinAnalyticsConfig()
        config.isLive = false

        val isLive = Util.getIsLiveFromConfigOrPlayer(false, config.isLive, false)
        assertThat(isLive).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsPlayerIsLiveTrueIfPlayerReady() {
        val config = BitmovinAnalyticsConfig()
        config.isLive = true

        val isLive = Util.getIsLiveFromConfigOrPlayer(true, config.isLive, true)
        assertThat(isLive).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsPlayerIsLiveFalseIfPlayerReady() {
        val config = BitmovinAnalyticsConfig()
        config.isLive = true

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

    @Test
    fun multiplyInteger() {
        Assert.assertNull(Util.multiply(null, null as? Int?))
        Assert.assertNull(Util.multiply(null, 1))
        Assert.assertNull(Util.multiply(1.0, null as? Int?))
        Assert.assertEquals(Util.multiply(1.0, 1), 1.0, 0.0)
    }

    @Test
    fun multiplyLong() {
        Assert.assertNull(Util.multiply(null, null as? Long?))
        Assert.assertNull(Util.multiply(null, 1L))
        Assert.assertNull(Util.multiply(1.0, null as? Long?))
        Assert.assertEquals(Util.multiply(1.0, 1L), 1.0, 0.0)
    }

    @Test
    fun secondsToMillis() {
        Assert.assertEquals(Util.secondsToMillis(null), 0)
        Assert.assertEquals(Util.secondsToMillis(0.0), 0)
        Assert.assertEquals(Util.secondsToMillis(3.5), 3500)
    }
}
