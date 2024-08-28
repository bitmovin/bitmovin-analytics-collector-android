package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.data.EventData
import org.assertj.core.api.Assertions.assertThat

object SsaiDataVerifier {
    private const val SSAI_AD_TYPE = 2

    fun verifySamplesHaveSameAdIndex(
        adEventDataList: List<AdEventDataForTest>,
        adIndex: Int,
    ) {
        adEventDataList.forEach {
            if (it.adIndex != adIndex) {
                throw AssertionError("AdIndex is not correct. Expected: $adIndex, but was: ${it.adIndex}")
            }
        }
    }

    fun verifySamplesHaveSameAdSystem(
        adEventDataList: List<AdEventDataForTest>,
        adSystem: String,
    ) {
        adEventDataList.forEach {
            if (it.adSystem != adSystem) {
                throw AssertionError("adSystem is not correct. Expected: $adSystem, but was: ${it.adSystem}")
            }
        }
    }

    fun verifySamplesHaveSameAdId(
        adEventDataList: List<AdEventDataForTest>,
        adId: String,
    ) {
        adEventDataList.forEach {
            if (it.adId != adId) {
                throw AssertionError("adId is not correct. Expected: $adId, but was: ${it.adId}")
            }
        }
    }

    fun verifySamplesHaveBasicAdInfoSet(adEventDataList: List<AdEventDataForTest>) {
        adEventDataList.forEach {
            assertThat(it.adImpressionId).isNotEmpty()
            assertThat(it.adType).isEqualTo(SSAI_AD_TYPE)
            assertThat(it.analyticsVersion).isNotEmpty()
            assertThat(it.customUserId).isNotEmpty()
            assertThat(it.domain).isNotEmpty()
            assertThat(it.key).isNotEmpty()
            assertThat(it.language).isNotEmpty()
            assertThat(it.hasSsaiRoutingParamSet).isTrue()
            assertThat(it.path).isNotEmpty()
            assertThat(it.platform).isNotEmpty()
            assertThat(it.player).isNotEmpty()
            assertThat(it.playerKey).isNotEmpty()
            assertThat(it.playerTech).isNotEmpty()
            assertThat(it.screenHeight).isGreaterThan(0)
            assertThat(it.screenWidth).isGreaterThan(0)
            assertThat(it.streamFormat).isNotEmpty()
            assertThat(it.time).isGreaterThan(0)
            assertThat(it.userAgent).isNotEmpty()
            assertThat(it.userId).isNotEmpty()
            assertThat(it.version).isNotEmpty()
            assertThat(it.videoId).isNotEmpty()
            assertThat(it.videoImpressionId).isNotEmpty()
            assertThat(it.videoTitle).isNotEmpty()

            // we don't check for adPlaybackWidth and adPlaybackHeight
            // since tests are starting before play and thus they are not set
            // assertThat(it.adPlaybackHeight).isGreaterThan(0)
            // assertThat(it.adPlaybackWidth).isGreaterThan(0)
        }

        verifyFieldsAreTheSameOverAdImpression(adEventDataList)
    }

    private fun verifyFieldsAreTheSameOverAdImpression(adEventDataList: List<AdEventDataForTest>) {
        if (adEventDataList.size > 1) {
            val adImpressionId = adEventDataList[0].adImpressionId
            val analyticsVersion = adEventDataList[0].analyticsVersion
            val customUserId = adEventDataList[0].customUserId
            val domain = adEventDataList[0].domain
            val key = adEventDataList[0].key
            val language = adEventDataList[0].language
            val path = adEventDataList[0].path
            val platform = adEventDataList[0].platform
            val player = adEventDataList[0].player
            val playerKey = adEventDataList[0].playerKey
            val playerTech = adEventDataList[0].playerTech
            val streamFormat = adEventDataList[0].streamFormat
            val screenHeight = adEventDataList[0].screenHeight
            val screenWidth = adEventDataList[0].screenWidth
            val userAgent = adEventDataList[0].userAgent
            val userId = adEventDataList[0].userId
            val version = adEventDataList[0].version
            val videoId = adEventDataList[0].videoId
            val videoImpressionId = adEventDataList[0].videoImpressionId
            val videoTitle = adEventDataList[0].videoTitle

            adEventDataList.forEach {
                assertThat(it.adImpressionId).isEqualTo(adImpressionId)
                assertThat(it.analyticsVersion).isEqualTo(analyticsVersion)
                assertThat(it.customUserId).isEqualTo(customUserId)
                assertThat(it.domain).isEqualTo(domain)
                assertThat(it.language).isEqualTo(language)
                assertThat(it.key).isEqualTo(key)
                assertThat(it.path).isEqualTo(path)
                assertThat(it.platform).isEqualTo(platform)
                assertThat(it.player).isEqualTo(player)
                assertThat(it.playerKey).isEqualTo(playerKey)
                assertThat(it.playerTech).isEqualTo(playerTech)
                assertThat(it.streamFormat).isEqualTo(streamFormat)
                assertThat(it.screenHeight).isEqualTo(screenHeight)
                assertThat(it.screenWidth).isEqualTo(screenWidth)
                assertThat(it.userAgent).isEqualTo(userAgent)
                assertThat(it.userId).isEqualTo(userId)
                assertThat(it.version).isEqualTo(version)
                assertThat(it.videoId).isEqualTo(videoId)
                assertThat(it.videoImpressionId).isEqualTo(videoImpressionId)
                assertThat(it.videoTitle).isEqualTo(videoTitle)
            }
        }
    }

    fun verifySsaiRelatedSamplesHaveHeaderSet(eventDataList: List<EventData>) {
        // all ssai samples should have the routing key set
        // and the last sample before the ad block
        eventDataList.forEachIndexed { index, eventData ->
            if (eventData.ad == SSAI_AD_TYPE) {
                assertThat(eventData.ssaiRelatedSample).isTrue()

                val sampleBefore = index - 1

                // verify that sample before ad block has ssai header set
                // if sample is not startup sample
                if (sampleBefore >= 0 && eventDataList[sampleBefore].ad != SSAI_AD_TYPE &&
                    eventDataList[sampleBefore].videoStartupTime == 0L
                ) {
                    assertThat(eventDataList[sampleBefore].ssaiRelatedSample).isTrue()
                }
            }
        }

        for (eventData in eventDataList) {
            if (eventData.ad == SSAI_AD_TYPE) {
                // ssaiRelatedSample is a transitive property, but the
                // mocked ingress sets it according to header info
                // thus we can validate it here
                assertThat(eventData.ssaiRelatedSample).isTrue()
            }
        }
    }
}
