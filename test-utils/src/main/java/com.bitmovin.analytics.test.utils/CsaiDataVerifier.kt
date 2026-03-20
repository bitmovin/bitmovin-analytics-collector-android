package com.bitmovin.analytics.test.utils

import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import org.assertj.core.api.Assertions.assertThat

object CsaiDataVerifier {
    fun verifyStaticAdData(
        adSample: AdEventDataForTest,
        analyticsConfig: AnalyticsConfig,
        expectedPlayer: String = "bitmovin",
        sourceMetadata: SourceMetadata? = null,
    ) {
        verifyAdIndependentData(adSample, analyticsConfig, expectedPlayer)

        if (sourceMetadata != null) {
            verifySourceMetadata(adSample, sourceMetadata)
        }
    }

    fun verifyCustomData(
        adEventDataList: List<AdEventDataForTest>,
        customData: CustomData,
    ) {
        for (eventData in adEventDataList) {
            verifyCustomData(eventData, customData)
        }
    }

    fun verifyCustomData(
        eventData: AdEventDataForTest,
        expectedCustomData: CustomData,
    ) {
        assertThat(eventData.customData1).isEqualTo(expectedCustomData.customData1)
        assertThat(eventData.customData2).isEqualTo(expectedCustomData.customData2)
        assertThat(eventData.customData3).isEqualTo(expectedCustomData.customData3)
        assertThat(eventData.customData4).isEqualTo(expectedCustomData.customData4)
        assertThat(eventData.customData5).isEqualTo(expectedCustomData.customData5)
        assertThat(eventData.customData6).isEqualTo(expectedCustomData.customData6)
        assertThat(eventData.customData7).isEqualTo(expectedCustomData.customData7)
        assertThat(eventData.customData8).isEqualTo(expectedCustomData.customData8)
        assertThat(eventData.customData9).isEqualTo(expectedCustomData.customData9)
        assertThat(eventData.customData10).isEqualTo(expectedCustomData.customData10)
        assertThat(eventData.customData11).isEqualTo(expectedCustomData.customData11)
        assertThat(eventData.customData12).isEqualTo(expectedCustomData.customData12)
        assertThat(eventData.customData13).isEqualTo(expectedCustomData.customData13)
        assertThat(eventData.customData14).isEqualTo(expectedCustomData.customData14)
        assertThat(eventData.customData15).isEqualTo(expectedCustomData.customData15)
        assertThat(eventData.customData16).isEqualTo(expectedCustomData.customData16)
        assertThat(eventData.customData17).isEqualTo(expectedCustomData.customData17)
        assertThat(eventData.customData18).isEqualTo(expectedCustomData.customData18)
        assertThat(eventData.customData19).isEqualTo(expectedCustomData.customData19)
        assertThat(eventData.customData20).isEqualTo(expectedCustomData.customData20)
        assertThat(eventData.customData21).isEqualTo(expectedCustomData.customData21)
        assertThat(eventData.customData22).isEqualTo(expectedCustomData.customData22)
        assertThat(eventData.customData23).isEqualTo(expectedCustomData.customData23)
        assertThat(eventData.customData24).isEqualTo(expectedCustomData.customData24)
        assertThat(eventData.customData25).isEqualTo(expectedCustomData.customData25)
        assertThat(eventData.customData26).isEqualTo(expectedCustomData.customData26)
        assertThat(eventData.customData27).isEqualTo(expectedCustomData.customData27)
        assertThat(eventData.customData28).isEqualTo(expectedCustomData.customData28)
        assertThat(eventData.customData29).isEqualTo(expectedCustomData.customData29)
        assertThat(eventData.customData30).isEqualTo(expectedCustomData.customData30)
        assertThat(eventData.customData31).isEqualTo(expectedCustomData.customData31)
        assertThat(eventData.customData32).isEqualTo(expectedCustomData.customData32)
        assertThat(eventData.customData33).isEqualTo(expectedCustomData.customData33)
        assertThat(eventData.customData34).isEqualTo(expectedCustomData.customData34)
        assertThat(eventData.customData35).isEqualTo(expectedCustomData.customData35)
        assertThat(eventData.customData36).isEqualTo(expectedCustomData.customData36)
        assertThat(eventData.customData37).isEqualTo(expectedCustomData.customData37)
        assertThat(eventData.customData38).isEqualTo(expectedCustomData.customData38)
        assertThat(eventData.customData39).isEqualTo(expectedCustomData.customData39)
        assertThat(eventData.customData40).isEqualTo(expectedCustomData.customData40)
        assertThat(eventData.customData41).isEqualTo(expectedCustomData.customData41)
        assertThat(eventData.customData42).isEqualTo(expectedCustomData.customData42)
        assertThat(eventData.customData43).isEqualTo(expectedCustomData.customData43)
        assertThat(eventData.customData44).isEqualTo(expectedCustomData.customData44)
        assertThat(eventData.customData45).isEqualTo(expectedCustomData.customData45)
        assertThat(eventData.customData46).isEqualTo(expectedCustomData.customData46)
        assertThat(eventData.customData47).isEqualTo(expectedCustomData.customData47)
        assertThat(eventData.customData48).isEqualTo(expectedCustomData.customData48)
        assertThat(eventData.customData49).isEqualTo(expectedCustomData.customData49)
        assertThat(eventData.customData50).isEqualTo(expectedCustomData.customData50)
        assertThat(eventData.customData51).isEqualTo(expectedCustomData.customData51)
        assertThat(eventData.customData52).isEqualTo(expectedCustomData.customData52)
        assertThat(eventData.customData53).isEqualTo(expectedCustomData.customData53)
        assertThat(eventData.customData54).isEqualTo(expectedCustomData.customData54)
        assertThat(eventData.customData55).isEqualTo(expectedCustomData.customData55)
        assertThat(eventData.customData56).isEqualTo(expectedCustomData.customData56)
        assertThat(eventData.customData57).isEqualTo(expectedCustomData.customData57)
        assertThat(eventData.customData58).isEqualTo(expectedCustomData.customData58)
        assertThat(eventData.customData59).isEqualTo(expectedCustomData.customData59)
        assertThat(eventData.customData60).isEqualTo(expectedCustomData.customData60)
        assertThat(eventData.customData61).isEqualTo(expectedCustomData.customData61)
        assertThat(eventData.customData62).isEqualTo(expectedCustomData.customData62)
        assertThat(eventData.customData63).isEqualTo(expectedCustomData.customData63)
        assertThat(eventData.customData64).isEqualTo(expectedCustomData.customData64)
        assertThat(eventData.customData65).isEqualTo(expectedCustomData.customData65)
        assertThat(eventData.customData66).isEqualTo(expectedCustomData.customData66)
        assertThat(eventData.customData67).isEqualTo(expectedCustomData.customData67)
        assertThat(eventData.customData68).isEqualTo(expectedCustomData.customData68)
        assertThat(eventData.customData69).isEqualTo(expectedCustomData.customData69)
        assertThat(eventData.customData70).isEqualTo(expectedCustomData.customData70)
        assertThat(eventData.customData71).isEqualTo(expectedCustomData.customData71)
        assertThat(eventData.customData72).isEqualTo(expectedCustomData.customData72)
        assertThat(eventData.customData73).isEqualTo(expectedCustomData.customData73)
        assertThat(eventData.customData74).isEqualTo(expectedCustomData.customData74)
        assertThat(eventData.customData75).isEqualTo(expectedCustomData.customData75)
        assertThat(eventData.customData76).isEqualTo(expectedCustomData.customData76)
        assertThat(eventData.customData77).isEqualTo(expectedCustomData.customData77)
        assertThat(eventData.customData78).isEqualTo(expectedCustomData.customData78)
        assertThat(eventData.customData79).isEqualTo(expectedCustomData.customData79)
        assertThat(eventData.customData80).isEqualTo(expectedCustomData.customData80)
        assertThat(eventData.customData81).isEqualTo(expectedCustomData.customData81)
        assertThat(eventData.customData82).isEqualTo(expectedCustomData.customData82)
        assertThat(eventData.customData83).isEqualTo(expectedCustomData.customData83)
        assertThat(eventData.customData84).isEqualTo(expectedCustomData.customData84)
        assertThat(eventData.customData85).isEqualTo(expectedCustomData.customData85)
        assertThat(eventData.customData86).isEqualTo(expectedCustomData.customData86)
        assertThat(eventData.customData87).isEqualTo(expectedCustomData.customData87)
        assertThat(eventData.customData88).isEqualTo(expectedCustomData.customData88)
        assertThat(eventData.customData89).isEqualTo(expectedCustomData.customData89)
        assertThat(eventData.customData90).isEqualTo(expectedCustomData.customData90)
        assertThat(eventData.customData91).isEqualTo(expectedCustomData.customData91)
        assertThat(eventData.customData92).isEqualTo(expectedCustomData.customData92)
        assertThat(eventData.customData93).isEqualTo(expectedCustomData.customData93)
        assertThat(eventData.customData94).isEqualTo(expectedCustomData.customData94)
        assertThat(eventData.customData95).isEqualTo(expectedCustomData.customData95)
        assertThat(eventData.customData96).isEqualTo(expectedCustomData.customData96)
        assertThat(eventData.customData97).isEqualTo(expectedCustomData.customData97)
        assertThat(eventData.customData98).isEqualTo(expectedCustomData.customData98)
        assertThat(eventData.customData99).isEqualTo(expectedCustomData.customData99)
        assertThat(eventData.customData100).isEqualTo(expectedCustomData.customData100)
        assertThat(eventData.experimentName).isEqualTo(expectedCustomData.experimentName)
    }

    fun verifySourceMetadata(
        adEventData: AdEventDataForTest,
        sourceMetadata: SourceMetadata,
    ) {
        assertThat(adEventData.videoTitle).isEqualTo(sourceMetadata.title)
        assertThat(adEventData.videoId).isEqualTo(sourceMetadata.videoId)
        assertThat(adEventData.cdnProvider).isEqualTo(sourceMetadata.cdnProvider)
        assertThat(adEventData.path).isEqualTo(sourceMetadata.path)
        verifyCustomData(adEventData, sourceMetadata.customData)
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
