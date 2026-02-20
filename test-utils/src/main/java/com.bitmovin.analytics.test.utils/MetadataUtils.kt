package com.bitmovin.analytics.test.utils

import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import org.junit.rules.TestWatcher
import org.junit.runner.Description

object MetadataUtils {
    fun mergeSourceMetadata(
        sourceMetadata: SourceMetadata,
        defaultMetadata: DefaultMetadata,
    ): SourceMetadata {
        val mergedCustomData =
            mergeCustomData(sourceMetadata.customData, defaultMetadata.customData)

        return SourceMetadata(
            title = sourceMetadata.title,
            videoId = sourceMetadata.videoId,
            path = sourceMetadata.path,
            cdnProvider = sourceMetadata.cdnProvider ?: defaultMetadata.cdnProvider,
            customData = mergedCustomData,
        )
    }

    /**
     * This class is a toolkit to generate appropriate Stream metadata depending of the test context.
     * It's main feature is the `getTestTitle` method that returns the current test class+name.
     *
     * This have been created to make the tests more debuggable while not rewriting all names by hand.
     *
     * for the class to work, you need to add it as a rule in your test class:
     * ```kotlin
     * @get:Rule
     * val metadataGenerator = MetadataGenerator()
     * ... other cool stuffs ...
     * ```
     */
    class MetadataGenerator : TestWatcher() {
        companion object {
            var currentTestNameJVM = "__NONE__"

            /**
             * Returns a name representing the current test.
             *
             * This variant is meant to be used when we can't access the metadataGenerator instance directly in this context.
             * Should only be used if the test class has the MetadataGenerator rule.
             * @see MetadataGenerator
             */
            fun getTestTitle(addition: String? = null): String {
                return currentTestNameJVM + (addition?.let { " $it" } ?: "")
            }
        }

        var currentTestName = "__NONE__"

        /**
         * Should never be called directly. It's called by the JUnit framework when a test is starting.
         */
        public override fun starting(description: Description) {
            currentTestName = description.testClass.simpleName + " : " + description.methodName
            currentTestNameJVM = currentTestName
        }

        /**
         * Returns a name representing the current test.
         *
         * Should only be used if the test class has the MetadataGenerator rule.
         * @see MetadataGenerator
         */
        fun getTestTitle(
            /**
             * If the test create multiples videos, we should add an identifier to each videos.
             */
            addition: String? = null,
        ): String {
            return currentTestName + (addition?.let { " $it" } ?: "")
        }

        /**
         * Generate the whole SourceMetadata.
         *
         * Should only be used if the test class has the MetadataGenerator rule.
         * @see MetadataGenerator
         */
        fun generate(
            addition: String? = null,
            title: String? = currentTestName,
            videoId: String? = currentTestName + (addition?.let { "_$it" } ?: ""),
            path: String? = "testPath",
            cdnProvider: String? = "testCdnProvider",
            customData: CustomData = TestConfig.createDummyCustomData(),
            isLive: Boolean = false,
        ): SourceMetadata {
            return SourceMetadata(
                title = title + (addition?.let { " $it" } ?: ""),
                videoId = videoId,
                path = path,
                cdnProvider = cdnProvider,
                customData = customData,
                isLive = isLive,
            )
        }
    }

    fun mergeCustomData(
        mainCustomData: CustomData,
        fallbackCustomData: CustomData,
    ): CustomData {
        return CustomData(
            customData1 = mainCustomData.customData1 ?: fallbackCustomData.customData1,
            customData2 = mainCustomData.customData2 ?: fallbackCustomData.customData2,
            customData3 = mainCustomData.customData3 ?: fallbackCustomData.customData3,
            customData4 = mainCustomData.customData4 ?: fallbackCustomData.customData4,
            customData5 = mainCustomData.customData5 ?: fallbackCustomData.customData5,
            customData6 = mainCustomData.customData6 ?: fallbackCustomData.customData6,
            customData7 = mainCustomData.customData7 ?: fallbackCustomData.customData7,
            customData8 = mainCustomData.customData8 ?: fallbackCustomData.customData8,
            customData9 = mainCustomData.customData9 ?: fallbackCustomData.customData9,
            customData10 = mainCustomData.customData10 ?: fallbackCustomData.customData10,
            customData11 = mainCustomData.customData11 ?: fallbackCustomData.customData11,
            customData12 = mainCustomData.customData12 ?: fallbackCustomData.customData12,
            customData13 = mainCustomData.customData13 ?: fallbackCustomData.customData13,
            customData14 = mainCustomData.customData14 ?: fallbackCustomData.customData14,
            customData15 = mainCustomData.customData15 ?: fallbackCustomData.customData15,
            customData16 = mainCustomData.customData16 ?: fallbackCustomData.customData16,
            customData17 = mainCustomData.customData17 ?: fallbackCustomData.customData17,
            customData18 = mainCustomData.customData18 ?: fallbackCustomData.customData18,
            customData19 = mainCustomData.customData19 ?: fallbackCustomData.customData19,
            customData20 = mainCustomData.customData20 ?: fallbackCustomData.customData20,
            customData21 = mainCustomData.customData21 ?: fallbackCustomData.customData21,
            customData22 = mainCustomData.customData22 ?: fallbackCustomData.customData22,
            customData23 = mainCustomData.customData23 ?: fallbackCustomData.customData23,
            customData24 = mainCustomData.customData24 ?: fallbackCustomData.customData24,
            customData25 = mainCustomData.customData25 ?: fallbackCustomData.customData25,
            customData26 = mainCustomData.customData26 ?: fallbackCustomData.customData26,
            customData27 = mainCustomData.customData27 ?: fallbackCustomData.customData27,
            customData28 = mainCustomData.customData28 ?: fallbackCustomData.customData28,
            customData29 = mainCustomData.customData29 ?: fallbackCustomData.customData29,
            customData30 = mainCustomData.customData30 ?: fallbackCustomData.customData30,
            experimentName = mainCustomData.experimentName ?: fallbackCustomData.experimentName,
            customData31 = mainCustomData.customData31 ?: fallbackCustomData.customData31,
            customData32 = mainCustomData.customData32 ?: fallbackCustomData.customData32,
            customData33 = mainCustomData.customData33 ?: fallbackCustomData.customData33,
            customData34 = mainCustomData.customData34 ?: fallbackCustomData.customData34,
            customData35 = mainCustomData.customData35 ?: fallbackCustomData.customData35,
            customData36 = mainCustomData.customData36 ?: fallbackCustomData.customData36,
            customData37 = mainCustomData.customData37 ?: fallbackCustomData.customData37,
            customData38 = mainCustomData.customData38 ?: fallbackCustomData.customData38,
            customData39 = mainCustomData.customData39 ?: fallbackCustomData.customData39,
            customData40 = mainCustomData.customData40 ?: fallbackCustomData.customData40,
            customData41 = mainCustomData.customData41 ?: fallbackCustomData.customData41,
            customData42 = mainCustomData.customData42 ?: fallbackCustomData.customData42,
            customData43 = mainCustomData.customData43 ?: fallbackCustomData.customData43,
            customData44 = mainCustomData.customData44 ?: fallbackCustomData.customData44,
            customData45 = mainCustomData.customData45 ?: fallbackCustomData.customData45,
            customData46 = mainCustomData.customData46 ?: fallbackCustomData.customData46,
            customData47 = mainCustomData.customData47 ?: fallbackCustomData.customData47,
            customData48 = mainCustomData.customData48 ?: fallbackCustomData.customData48,
            customData49 = mainCustomData.customData49 ?: fallbackCustomData.customData49,
            customData50 = mainCustomData.customData50 ?: fallbackCustomData.customData50,
            customData51 = mainCustomData.customData51 ?: fallbackCustomData.customData51,
            customData52 = mainCustomData.customData52 ?: fallbackCustomData.customData52,
            customData53 = mainCustomData.customData53 ?: fallbackCustomData.customData53,
            customData54 = mainCustomData.customData54 ?: fallbackCustomData.customData54,
            customData55 = mainCustomData.customData55 ?: fallbackCustomData.customData55,
            customData56 = mainCustomData.customData56 ?: fallbackCustomData.customData56,
            customData57 = mainCustomData.customData57 ?: fallbackCustomData.customData57,
            customData58 = mainCustomData.customData58 ?: fallbackCustomData.customData58,
            customData59 = mainCustomData.customData59 ?: fallbackCustomData.customData59,
            customData60 = mainCustomData.customData60 ?: fallbackCustomData.customData60,
            customData61 = mainCustomData.customData61 ?: fallbackCustomData.customData61,
            customData62 = mainCustomData.customData62 ?: fallbackCustomData.customData62,
            customData63 = mainCustomData.customData63 ?: fallbackCustomData.customData63,
            customData64 = mainCustomData.customData64 ?: fallbackCustomData.customData64,
            customData65 = mainCustomData.customData65 ?: fallbackCustomData.customData65,
            customData66 = mainCustomData.customData66 ?: fallbackCustomData.customData66,
            customData67 = mainCustomData.customData67 ?: fallbackCustomData.customData67,
            customData68 = mainCustomData.customData68 ?: fallbackCustomData.customData68,
            customData69 = mainCustomData.customData69 ?: fallbackCustomData.customData69,
            customData70 = mainCustomData.customData70 ?: fallbackCustomData.customData70,
            customData71 = mainCustomData.customData71 ?: fallbackCustomData.customData71,
            customData72 = mainCustomData.customData72 ?: fallbackCustomData.customData72,
            customData73 = mainCustomData.customData73 ?: fallbackCustomData.customData73,
            customData74 = mainCustomData.customData74 ?: fallbackCustomData.customData74,
            customData75 = mainCustomData.customData75 ?: fallbackCustomData.customData75,
            customData76 = mainCustomData.customData76 ?: fallbackCustomData.customData76,
            customData77 = mainCustomData.customData77 ?: fallbackCustomData.customData77,
            customData78 = mainCustomData.customData78 ?: fallbackCustomData.customData78,
            customData79 = mainCustomData.customData79 ?: fallbackCustomData.customData79,
            customData80 = mainCustomData.customData80 ?: fallbackCustomData.customData80,
            customData81 = mainCustomData.customData81 ?: fallbackCustomData.customData81,
            customData82 = mainCustomData.customData82 ?: fallbackCustomData.customData82,
            customData83 = mainCustomData.customData83 ?: fallbackCustomData.customData83,
            customData84 = mainCustomData.customData84 ?: fallbackCustomData.customData84,
            customData85 = mainCustomData.customData85 ?: fallbackCustomData.customData85,
            customData86 = mainCustomData.customData86 ?: fallbackCustomData.customData86,
            customData87 = mainCustomData.customData87 ?: fallbackCustomData.customData87,
            customData88 = mainCustomData.customData88 ?: fallbackCustomData.customData88,
            customData89 = mainCustomData.customData89 ?: fallbackCustomData.customData89,
            customData90 = mainCustomData.customData90 ?: fallbackCustomData.customData90,
            customData91 = mainCustomData.customData91 ?: fallbackCustomData.customData91,
            customData92 = mainCustomData.customData92 ?: fallbackCustomData.customData92,
            customData93 = mainCustomData.customData93 ?: fallbackCustomData.customData93,
            customData94 = mainCustomData.customData94 ?: fallbackCustomData.customData94,
            customData95 = mainCustomData.customData95 ?: fallbackCustomData.customData95,
            customData96 = mainCustomData.customData96 ?: fallbackCustomData.customData96,
            customData97 = mainCustomData.customData97 ?: fallbackCustomData.customData97,
            customData98 = mainCustomData.customData98 ?: fallbackCustomData.customData98,
            customData99 = mainCustomData.customData99 ?: fallbackCustomData.customData99,
            customData100 = mainCustomData.customData100 ?: fallbackCustomData.customData100,
        )
    }
}
