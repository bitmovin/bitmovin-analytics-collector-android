package com.bitmovin.analytics.utils;

import com.bitmovin.analytics.CollectorConfig
import org.junit.Test
import java.lang.Exception
import org.assertj.core.api.Assertions.*

class UtilTest {

    @Test
    fun testTopOfStacktrace() {
        try {
            throw RuntimeException("RUNTIMEEXCEPTION")
        } catch (e : Exception){
            val top = e.topOfStacktrace
            assertThat(top).hasSize(10)
            assertThat(top).anySatisfy{ element -> assertThat(element).contains("testTopOfStacktrace")}
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsConfigValueTrueIfPlayerNotReady() {
        val config = CollectorConfig()
        config.setIsLive(true)

        val isLive = Util.getIsLiveFromConfigOrPlayer(false, config, false)
        assertThat(isLive).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsConfigValueFalseIfPlayerNotReady() {
        val config = CollectorConfig()
        config.setIsLive(false)

        val isLive = Util.getIsLiveFromConfigOrPlayer(false, config, false)
        assertThat(isLive).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsPlayerIsLiveTrueIfPlayerReady() {
        val config = CollectorConfig()
        config.setIsLive(true)

        val isLive = Util.getIsLiveFromConfigOrPlayer(true, config, true)
        assertThat(isLive).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testGetIsLiveFromConfigOrPlayer_ReturnsPlayerIsLiveFalseIfPlayerReady() {
        val config = CollectorConfig()
        config.setIsLive(true)

        val isLive = Util.getIsLiveFromConfigOrPlayer(true, config, false)
        assertThat(isLive).isFalse()
    }
}
