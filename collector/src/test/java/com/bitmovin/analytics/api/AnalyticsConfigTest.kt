package com.bitmovin.analytics.api

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import org.junit.Assert
import org.junit.Test
import java.util.UUID

class AnalyticsConfigTest {

    private fun randomString(): String = UUID.randomUUID().toString()

    @Test
    fun testAnalyticsConfigMapping() {
        val config = AnalyticsConfig(
            randomString(),
            playerKey = randomString(),
            cdnProvider = randomString(),
            customData1 = randomString(),
            customData2 = randomString(),
            customData3 = randomString(),
            customData4 = randomString(),
            customData5 = randomString(),
            customData6 = randomString(),
            customData7 = randomString(),
            customData8 = randomString(),
            customData9 = randomString(),
            customData10 = randomString(),
            customData11 = randomString(),
            customData12 = randomString(),
            customData13 = randomString(),
            customData14 = randomString(),
            customData15 = randomString(),
            customData16 = randomString(),
            customData17 = randomString(),
            customData18 = randomString(),
            customData19 = randomString(),
            customData20 = randomString(),
            customData21 = randomString(),
            customData22 = randomString(),
            customData23 = randomString(),
            customData24 = randomString(),
            customData25 = randomString(),
            customData26 = randomString(),
            customData27 = randomString(),
            customData28 = randomString(),
            customData29 = randomString(),
            customData30 = randomString(),
            customUserId = randomString(),
            experimentName = randomString(),
            ads = true,
            randomizeUserId = true,
            backendUrl = randomString(),
            tryResendDataOnFailedConnection = true
        )

        val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig(config)

        Assert.assertEquals(config.playerKey, bitmovinAnalyticsConfig.playerKey)
        Assert.assertEquals(config.cdnProvider, bitmovinAnalyticsConfig.cdnProvider)
        Assert.assertEquals(config.customData1, bitmovinAnalyticsConfig.customData1)
        Assert.assertEquals(config.customData2, bitmovinAnalyticsConfig.customData2)
        Assert.assertEquals(config.customData3, bitmovinAnalyticsConfig.customData3)
        Assert.assertEquals(config.customData4, bitmovinAnalyticsConfig.customData4)
        Assert.assertEquals(config.customData5, bitmovinAnalyticsConfig.customData5)
        Assert.assertEquals(config.customData6, bitmovinAnalyticsConfig.customData6)
        Assert.assertEquals(config.customData7, bitmovinAnalyticsConfig.customData7)
        Assert.assertEquals(config.customData8, bitmovinAnalyticsConfig.customData8)
        Assert.assertEquals(config.customData9, bitmovinAnalyticsConfig.customData9)
        Assert.assertEquals(config.customData10, bitmovinAnalyticsConfig.customData10)
        Assert.assertEquals(config.customData11, bitmovinAnalyticsConfig.customData11)
        Assert.assertEquals(config.customData12, bitmovinAnalyticsConfig.customData12)
        Assert.assertEquals(config.customData13, bitmovinAnalyticsConfig.customData13)
        Assert.assertEquals(config.customData14, bitmovinAnalyticsConfig.customData14)
        Assert.assertEquals(config.customData15, bitmovinAnalyticsConfig.customData15)
        Assert.assertEquals(config.customData16, bitmovinAnalyticsConfig.customData16)
        Assert.assertEquals(config.customData17, bitmovinAnalyticsConfig.customData17)
        Assert.assertEquals(config.customData18, bitmovinAnalyticsConfig.customData18)
        Assert.assertEquals(config.customData19, bitmovinAnalyticsConfig.customData19)
        Assert.assertEquals(config.customData20, bitmovinAnalyticsConfig.customData20)
        Assert.assertEquals(config.customData21, bitmovinAnalyticsConfig.customData21)
        Assert.assertEquals(config.customData22, bitmovinAnalyticsConfig.customData22)
        Assert.assertEquals(config.customData23, bitmovinAnalyticsConfig.customData23)
        Assert.assertEquals(config.customData24, bitmovinAnalyticsConfig.customData24)
        Assert.assertEquals(config.customData25, bitmovinAnalyticsConfig.customData25)
        Assert.assertEquals(config.customData26, bitmovinAnalyticsConfig.customData26)
        Assert.assertEquals(config.customData27, bitmovinAnalyticsConfig.customData27)
        Assert.assertEquals(config.customData28, bitmovinAnalyticsConfig.customData28)
        Assert.assertEquals(config.customData29, bitmovinAnalyticsConfig.customData29)
        Assert.assertEquals(config.customData30, bitmovinAnalyticsConfig.customData30)
        Assert.assertEquals(config.customUserId, bitmovinAnalyticsConfig.customUserId)
        Assert.assertEquals(config.experimentName, bitmovinAnalyticsConfig.experimentName)
        Assert.assertEquals(config.ads, bitmovinAnalyticsConfig.ads)
        Assert.assertEquals(config.randomizeUserId, bitmovinAnalyticsConfig.randomizeUserId)
        Assert.assertEquals(config.backendUrl, bitmovinAnalyticsConfig.config.backendUrl)
        Assert.assertEquals(
            config.tryResendDataOnFailedConnection,
            bitmovinAnalyticsConfig.config.tryResendDataOnFailedConnection
        )
    }
}