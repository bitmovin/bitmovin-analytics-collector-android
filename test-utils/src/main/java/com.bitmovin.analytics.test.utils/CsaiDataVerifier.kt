package com.bitmovin.analytics.test.utils

import com.bitmovin.analytics.api.AnalyticsConfig
import org.assertj.core.api.Assertions.assertThat

object CsaiDataVerifier {
    fun verifyStaticAdData(
        adSample: AdEventDataForTest,
        analyticsConfig: AnalyticsConfig,
        expectedPlayer: String = "bitmovin",
    ) {
        verifyAdIndependentData(adSample, analyticsConfig, expectedPlayer)
    }

    fun verifyFullyPlayedAd(adSample: AdEventDataForTest) {
        assertThat(adSample.started).isEqualTo(1)
        assertThat(adSample.quartile1).isEqualTo(1)
        assertThat(adSample.midpoint).isEqualTo(1)
        assertThat(adSample.quartile3).isEqualTo(1)
        assertThat(adSample.completed).isEqualTo(1)
        assertThat(adSample.playPercentage).isEqualTo(100)
    }

    // basic fields that should always be set independent of the ad
    private fun verifyAdIndependentData(
        adSample: AdEventDataForTest,
        analyticsConfig: AnalyticsConfig,
        expectedPlayer: String,
    ) {
        assertThat(adSample.adType).isEqualTo(1)
        assertThat(adSample.adImpressionId).isNotBlank()
        assertThat(adSample.platform).isEqualTo("android")

        assertThat(adSample.version).isNotBlank() // aka playerVersion
        assertThat(adSample.userId).isNotBlank()
        assertThat(adSample.analyticsVersion).isNotBlank()
        assertThat(adSample.key).isEqualTo(analyticsConfig.licenseKey)
        assertThat(adSample.domain).isNotEmpty()
        assertThat(adSample.screenHeight).isGreaterThan(0)
        assertThat(adSample.screenWidth).isGreaterThan(0)
        assertThat(adSample.language).isNotBlank()
        assertThat(adSample.userAgent).isNotBlank()
        assertThat(adSample.playerTech).isNotBlank()
        assertThat(adSample.pageLoadType).isEqualTo(1)
        assertThat(adSample.player).isEqualTo(expectedPlayer)

        if (expectedPlayer == "bitmovin") {
            assertThat(adSample.key).isNotEmpty
        }
    }
}
