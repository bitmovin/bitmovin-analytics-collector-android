package com.bitmovin.analytics.systemtest.utils

import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.dtos.EventData
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

    fun verifyCustomData(
        adEventData: List<AdEventDataForTest>,
        expectedCustomData: CustomData,
    ) {
        adEventData.forEach {
            verifyCustomData(it, expectedCustomData)
        }
    }

    fun verifyCustomData(
        adEventData: AdEventDataForTest,
        expectedCustomData: CustomData,
    ) {
        assertThat(adEventData.customData1).isEqualTo(expectedCustomData.customData1)
        assertThat(adEventData.customData2).isEqualTo(expectedCustomData.customData2)
        assertThat(adEventData.customData3).isEqualTo(expectedCustomData.customData3)
        assertThat(adEventData.customData4).isEqualTo(expectedCustomData.customData4)
        assertThat(adEventData.customData5).isEqualTo(expectedCustomData.customData5)
        assertThat(adEventData.customData6).isEqualTo(expectedCustomData.customData6)
        assertThat(adEventData.customData7).isEqualTo(expectedCustomData.customData7)
        assertThat(adEventData.customData8).isEqualTo(expectedCustomData.customData8)
        assertThat(adEventData.customData9).isEqualTo(expectedCustomData.customData9)
        assertThat(adEventData.customData10).isEqualTo(expectedCustomData.customData10)
        assertThat(adEventData.customData11).isEqualTo(expectedCustomData.customData11)
        assertThat(adEventData.customData12).isEqualTo(expectedCustomData.customData12)
        assertThat(adEventData.customData13).isEqualTo(expectedCustomData.customData13)
        assertThat(adEventData.customData14).isEqualTo(expectedCustomData.customData14)
        assertThat(adEventData.customData15).isEqualTo(expectedCustomData.customData15)
        assertThat(adEventData.customData16).isEqualTo(expectedCustomData.customData16)
        assertThat(adEventData.customData17).isEqualTo(expectedCustomData.customData17)
        assertThat(adEventData.customData18).isEqualTo(expectedCustomData.customData18)
        assertThat(adEventData.customData19).isEqualTo(expectedCustomData.customData19)
        assertThat(adEventData.customData20).isEqualTo(expectedCustomData.customData20)
        assertThat(adEventData.customData21).isEqualTo(expectedCustomData.customData21)
        assertThat(adEventData.customData22).isEqualTo(expectedCustomData.customData22)
        assertThat(adEventData.customData23).isEqualTo(expectedCustomData.customData23)
        assertThat(adEventData.customData24).isEqualTo(expectedCustomData.customData24)
        assertThat(adEventData.customData25).isEqualTo(expectedCustomData.customData25)
        assertThat(adEventData.customData26).isEqualTo(expectedCustomData.customData26)
        assertThat(adEventData.customData27).isEqualTo(expectedCustomData.customData27)
        assertThat(adEventData.customData28).isEqualTo(expectedCustomData.customData28)
        assertThat(adEventData.customData29).isEqualTo(expectedCustomData.customData29)
        assertThat(adEventData.customData30).isEqualTo(expectedCustomData.customData30)
        assertThat(adEventData.customData31).isEqualTo(expectedCustomData.customData31)
        assertThat(adEventData.customData32).isEqualTo(expectedCustomData.customData32)
        assertThat(adEventData.customData33).isEqualTo(expectedCustomData.customData33)
        assertThat(adEventData.customData34).isEqualTo(expectedCustomData.customData34)
        assertThat(adEventData.customData35).isEqualTo(expectedCustomData.customData35)
        assertThat(adEventData.customData36).isEqualTo(expectedCustomData.customData36)
        assertThat(adEventData.customData37).isEqualTo(expectedCustomData.customData37)
        assertThat(adEventData.customData38).isEqualTo(expectedCustomData.customData38)
        assertThat(adEventData.customData39).isEqualTo(expectedCustomData.customData39)
        assertThat(adEventData.customData40).isEqualTo(expectedCustomData.customData40)
        assertThat(adEventData.customData41).isEqualTo(expectedCustomData.customData41)
        assertThat(adEventData.customData42).isEqualTo(expectedCustomData.customData42)
        assertThat(adEventData.customData43).isEqualTo(expectedCustomData.customData43)
        assertThat(adEventData.customData44).isEqualTo(expectedCustomData.customData44)
        assertThat(adEventData.customData45).isEqualTo(expectedCustomData.customData45)
        assertThat(adEventData.customData46).isEqualTo(expectedCustomData.customData46)
        assertThat(adEventData.customData47).isEqualTo(expectedCustomData.customData47)
        assertThat(adEventData.customData48).isEqualTo(expectedCustomData.customData48)
        assertThat(adEventData.customData49).isEqualTo(expectedCustomData.customData49)
        assertThat(adEventData.customData50).isEqualTo(expectedCustomData.customData50)
        assertThat(adEventData.experimentName).isEqualTo(expectedCustomData.experimentName)
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

    fun getSsaiAdEventSampleByAdIndex(
        adEventDataList: List<AdEventDataForTest>,
        adIndex: Int,
    ): AdEventDataForTest {
        adEventDataList.forEach {
            if (it.adIndex == adIndex) {
                return it
            }
        }

        throw RuntimeException("SSAI AdEvent with index = $adIndex not found")
    }
}
