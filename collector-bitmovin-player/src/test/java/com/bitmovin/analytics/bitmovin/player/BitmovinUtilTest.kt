package com.bitmovin.analytics.bitmovin.player

import com.bitmovin.player.BuildConfig
import org.junit.Assert
import org.junit.Test

class BitmovinUtilTest {
    @Test
    fun getPlayerVersion_returnsValueFromPlayerBuildConfig() {
        Assert.assertEquals(BuildConfig.VERSION_NAME, BitmovinUtil.playerVersion)
    }
}
