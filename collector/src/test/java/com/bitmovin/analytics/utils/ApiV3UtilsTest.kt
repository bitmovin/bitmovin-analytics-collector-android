package com.bitmovin.analytics.utils

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.RetryPolicy
import com.bitmovin.analytics.api.SourceMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ApiV3UtilsTest {
    @Test
    fun test_extractAnalyticsConfig_mapsAllFields() {
        // arrange
        val oldConfig =
            BitmovinAnalyticsConfig("key").apply {
                ads = false
                randomizeUserId = true
                config.backendUrl = "backendUrl"
                config.tryResendDataOnFailedConnection = true
                config.longTermRetryEnabled = true
            }

        // act
        val newConfig = ApiV3Utils.extractAnalyticsConfig(oldConfig)

        // assert
        val expectedConfig =
            AnalyticsConfig(
                licenseKey = "key",
                adTrackingDisabled = true,
                randomizeUserId = true,
                backendUrl = "backendUrl",
                retryPolicy = RetryPolicy.LONG_TERM,
            )
        assertThat(newConfig).isEqualTo(expectedConfig)
    }

    @Test
    fun test_extractDefaultMetadata_mapsAllFields() {
        // arrange
        val oldConfig =
            BitmovinAnalyticsConfig().apply {
                customData1 = "customData1"
                customData2 = "customData2"
                customData3 = "customData3"
                customData4 = "customData4"
                customData5 = "customData5"
                customData6 = "customData6"
                customData7 = "customData7"
                customData8 = "customData8"
                customData9 = "customData9"
                customData10 = "customData10"
                customData11 = "customData11"
                customData12 = "customData12"
                customData13 = "customData13"
                customData14 = "customData14"
                customData15 = "customData15"
                customData16 = "customData16"
                customData17 = "customData17"
                customData18 = "customData18"
                customData19 = "customData19"
                customData20 = "customData20"
                customData21 = "customData21"
                customData22 = "customData22"
                customData23 = "customData23"
                customData24 = "customData24"
                customData25 = "customData25"
                customData26 = "customData26"
                customData27 = "customData27"
                customData28 = "customData28"
                customData29 = "customData29"
                customData30 = "customData30"
                customData31 = "customData31"
                customData32 = "customData32"
                customData33 = "customData33"
                customData34 = "customData34"
                customData35 = "customData35"
                customData36 = "customData36"
                customData37 = "customData37"
                customData38 = "customData38"
                customData39 = "customData39"
                customData40 = "customData40"
                customData41 = "customData41"
                customData42 = "customData42"
                customData43 = "customData43"
                customData44 = "customData44"
                customData45 = "customData45"
                customData46 = "customData46"
                customData47 = "customData47"
                customData48 = "customData48"
                customData49 = "customData49"
                customData50 = "customData50"
                experimentName = "experimentName"
                customUserId = "customUserId"
                cdnProvider = "cdnProvider"
            }

        // act
        val defaultMetadata = ApiV3Utils.extractDefaultMetadata(oldConfig)

        val expectedCustomData =
            CustomData(
                customData1 = "customData1",
                customData2 = "customData2",
                customData3 = "customData3",
                customData4 = "customData4",
                customData5 = "customData5",
                customData6 = "customData6",
                customData7 = "customData7",
                customData8 = "customData8",
                customData9 = "customData9",
                customData10 = "customData10",
                customData11 = "customData11",
                customData12 = "customData12",
                customData13 = "customData13",
                customData14 = "customData14",
                customData15 = "customData15",
                customData16 = "customData16",
                customData17 = "customData17",
                customData18 = "customData18",
                customData19 = "customData19",
                customData20 = "customData20",
                customData21 = "customData21",
                customData22 = "customData22",
                customData23 = "customData23",
                customData24 = "customData24",
                customData25 = "customData25",
                customData26 = "customData26",
                customData27 = "customData27",
                customData28 = "customData28",
                customData29 = "customData29",
                customData30 = "customData30",
                customData31 = "customData31",
                customData32 = "customData32",
                customData33 = "customData33",
                customData34 = "customData34",
                customData35 = "customData35",
                customData36 = "customData36",
                customData37 = "customData37",
                customData38 = "customData38",
                customData39 = "customData39",
                customData40 = "customData40",
                customData41 = "customData41",
                customData42 = "customData42",
                customData43 = "customData43",
                customData44 = "customData44",
                customData45 = "customData45",
                customData46 = "customData46",
                customData47 = "customData47",
                customData48 = "customData48",
                customData49 = "customData49",
                customData50 = "customData50",
                experimentName = "experimentName",
            )

        // assert
        val expectedDefaultMetadata =
            DefaultMetadata(
                customUserId = "customUserId",
                customData = expectedCustomData,
                cdnProvider = "cdnProvider",
            )
        assertThat(defaultMetadata).isEqualTo(expectedDefaultMetadata)
    }

    @Test
    fun test_extractSourceMetadata_mapsAllFields() {
        // arrange
        val oldConfig =
            BitmovinAnalyticsConfig().apply {
                title = "title"
                videoId = "videoId"
                path = "path"
                cdnProvider = "cdnProvider"
                mpdUrl = "mpdUrl"
                m3u8Url = "m3u8Url"
                isLive = true
                progUrl = "progUrl"
            }
        // act
        val sourceMetadata = ApiV3Utils.extractSourceMetadata(oldConfig)

        // assert
        val expectedSourceMetadata =
            SourceMetadata(
                title = "title",
                videoId = "videoId",
                path = "path",
                cdnProvider = "cdnProvider",
                isLive = true,
            )
        assertThat(sourceMetadata).isEqualTo(expectedSourceMetadata)
    }

    @Test
    fun test_mergeSourceMetadata_mergedFieldsCorrectly1() {
        // arrange
        val sourceMetadata =
            SourceMetadata(
                title = "title",
                videoId = "videoId",
                path = "path",
                cdnProvider = "cdnProvider",
                isLive = true,
                customData = createDummyCustomData("test"),
            )

        val defaultMetadata =
            DefaultMetadata(
                customUserId = "customUserId",
                customData = createDummyCustomData("fallback"),
                cdnProvider = "cdnProvider",
            )

        // act
        val mergedSourceMetadata = ApiV3Utils.mergeSourceMetadata(sourceMetadata = sourceMetadata, defaultMetadata = defaultMetadata)

        // assert
        // since all fields are set in SourceMetadata, defaultMetadata shouldn't be used
        assertThat(mergedSourceMetadata).isEqualTo(sourceMetadata)
    }

    @Test
    fun test_mergeSourceMetadata_mergedFieldsCorrectly2() {
        // arrange
        val sourceMetadata =
            SourceMetadata(
                title = "title",
                videoId = "videoId",
                path = "path",
                customData = CustomData(customData1 = "test1", customData30 = "test30", customData50 = "test50"),
            )

        val defaultMetadata =
            DefaultMetadata(
                customUserId = "customUserId",
                customData = createDummyCustomData("fallback"),
                cdnProvider = "cdnProviderFallback",
            )

        val expectedCustomData =
            defaultMetadata.customData.copy(
                customData1 = "test1",
                customData30 = "test30",
                customData50 = "test50",
            )
        val expectedSourceMetadata =
            sourceMetadata.copy(
                cdnProvider = "cdnProviderFallback",
                customData = expectedCustomData,
            )

        // act
        val mergedSourceMetadata = ApiV3Utils.mergeSourceMetadata(sourceMetadata = sourceMetadata, defaultMetadata = defaultMetadata)

        // assert
        // since all fields are set in SourceMetadata, defaultMetadata shouldn't be used
        assertThat(mergedSourceMetadata).isEqualTo(expectedSourceMetadata)
    }

    @Test
    fun test_mergeCustomData_mergesFieldsCorrectly1() {
        // arrage
        val mainCustomData = createDummyCustomData("test")
        val fallbackCustomData = createDummyCustomData("fallback")

        // act
        val mergedCustomData = ApiV3Utils.mergeCustomData(mainCustomData, fallbackCustomData)

        // assert
        assertThat(mergedCustomData).isEqualTo(mainCustomData)
    }

    @Test
    fun test_mergeCustomData_mergesFieldsCorrectly2() {
        // arrage
        val mainCustomData =
            createDummyCustomData("test")
                .copy(customData1 = null, customData10 = null, customData30 = null, customData50 = null)
        val fallbackCustomData = createDummyCustomData("fallback")

        // act
        val mergedCustomData = ApiV3Utils.mergeCustomData(mainCustomData, fallbackCustomData)

        // assert
        val expectedCustomData =
            mainCustomData.copy(
                customData1 = "fallback1",
                customData10 = "fallback10",
                customData30 = "fallback30",
                customData50 = "fallback50",
            )
        assertThat(mergedCustomData).isEqualTo(expectedCustomData)
    }

    @Test
    fun test_mergeCustomData_mergesFieldsCorrectly3() {
        // arrage
        val mainCustomData = CustomData()
        val fallbackCustomData = createDummyCustomData("fallback")

        // act
        val mergedCustomData = ApiV3Utils.mergeCustomData(mainCustomData, fallbackCustomData)

        // assert
        assertThat(mergedCustomData).isEqualTo(fallbackCustomData)
    }

    companion object {
        fun createDummyCustomData(prefix: String = "customData"): CustomData {
            return CustomData(
                experimentName = prefix + "Experiment", customData1 = prefix + "1", customData2 = prefix + "2",
                customData3 = prefix + "3", customData4 = prefix + "4", customData5 = prefix + "5",
                customData6 = prefix + "6", customData7 = prefix + "7", customData8 = prefix + "8",
                customData9 = prefix + "9", customData10 = prefix + "10", customData11 = prefix + "11",
                customData12 = prefix + "12", customData13 = prefix + "13", customData14 = prefix + "14",
                customData15 = prefix + "15", customData16 = prefix + "16", customData17 = prefix + "17",
                customData18 = prefix + "18", customData19 = prefix + "19", customData20 = prefix + "20",
                customData21 = prefix + "21", customData22 = prefix + "22", customData23 = prefix + "23",
                customData24 = prefix + "24", customData25 = prefix + "25", customData26 = prefix + "26",
                customData27 = prefix + "27", customData28 = prefix + "28", customData29 = prefix + "29",
                customData30 = prefix + "30", customData31 = prefix + "31", customData32 = prefix + "32",
                customData33 = prefix + "33", customData34 = prefix + "34", customData35 = prefix + "35",
                customData36 = prefix + "36", customData37 = prefix + "37", customData38 = prefix + "38",
                customData39 = prefix + "39", customData40 = prefix + "40", customData41 = prefix + "41",
                customData42 = prefix + "42", customData43 = prefix + "43", customData44 = prefix + "34",
                customData45 = prefix + "45", customData46 = prefix + "46", customData47 = prefix + "47",
                customData48 = prefix + "48", customData49 = prefix + "49", customData50 = prefix + "50",
            )
        }
    }
}
