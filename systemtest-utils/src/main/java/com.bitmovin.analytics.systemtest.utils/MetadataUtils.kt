package com.bitmovin.analytics.systemtest.utils

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
            path: String? = null,
            cdnProvider: String? = null,
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
        )
    }
}
