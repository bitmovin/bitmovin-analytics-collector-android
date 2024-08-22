package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.data.AdEventData
import org.assertj.core.api.Assertions.assertThat

object SsaiDataVerifier {
    private const val SSAI_AD_TYPE = 2

    fun verifySamplesHaveSameAdIndex(
        adEventDataList: List<AdEventData>,
        adIndex: Int,
    ) {
        adEventDataList.forEach {
            if (it.adIndex != adIndex) {
                throw AssertionError("AdIndex is not correct. Expected: $adIndex, but was: ${it.adIndex}")
            }
        }
    }

    fun verifySamplesHaveSameAdSystem(
        adEventDataList: List<AdEventData>,
        adSystem: String,
    ) {
        adEventDataList.forEach {
            if (it.adSystem != adSystem) {
                throw AssertionError("adSystem is not correct. Expected: $adSystem, but was: ${it.adSystem}")
            }
        }
    }

    fun verifySamplesHaveSameAdId(
        adEventDataList: List<AdEventData>,
        adId: String,
    ) {
        adEventDataList.forEach {
            if (it.adId != adId) {
                throw AssertionError("adId is not correct. Expected: $adId, but was: ${it.adId}")
            }
        }
    }

    fun verifySamplesHaveBasicAdInfoSet(adEventDataList: List<AdEventData>) {
        // TODO: we might also check that these fields are the same for all samples
        adEventDataList.forEach {
            assertThat(it.adImpressionId).isNotEmpty()
            assertThat(it.videoImpressionId).isNotEmpty()
            assertThat(it.analyticsVersion).isNotEmpty()
            assertThat(it.userId).isNotEmpty()
            assertThat(it.adType).isEqualTo(SSAI_AD_TYPE)
            assertThat(it.playerTech).isNotEmpty()
            assertThat(it.player).isNotEmpty()
            assertThat(it.key).isNotEmpty()
            assertThat(it.platform).isNotEmpty()
            assertThat(it.domain).isNotEmpty()
            assertThat(it.userAgent).isNotEmpty()
            assertThat(it.time).isGreaterThan(0)
            assertThat(it.playerKey).isNotEmpty()
            assertThat(it.version).isNotEmpty()
            assertThat(it.screenHeight).isGreaterThan(0)
            assertThat(it.screenWidth).isGreaterThan(0)
            assertThat(it.time).isGreaterThan(0)
            assertThat(it.language).isNotEmpty()
            assertThat(it.streamFormat).isNotEmpty()
            //  assertThat(it.path).isNotEmpty()
        }
    }
}
