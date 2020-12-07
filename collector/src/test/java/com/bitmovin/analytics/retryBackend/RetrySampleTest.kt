package com.bitmovin.analytics.retryBackend


import com.bitmovin.analytics.BitmovinAnalyticsConfig

import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.manipulators.ManifestUrlEventDataManipulator
import org.assertj.core.api.Assertions
import org.junit.Test
import java.util.*

class RetrySampleTest {



    @Test
    fun testSamplesShouldBeOrderedByScheduledTime() {
        // #region Mocking
        var retrySamplesSet = sortedSetOf<RetrySample<String>>()
        val firstDate = Calendar.getInstance().run {
            add(Calendar.HOUR, 1)
            time
        }

        val secondDate = Calendar.getInstance().run {
            add(Calendar.HOUR, 2)
            time
        }

        val thirdDate = Calendar.getInstance().run {
            add(Calendar.HOUR, 3)
            time
        }

        val fourthDate = Calendar.getInstance().run {
            add(Calendar.HOUR, 4)
            time
        }

        retrySamplesSet.add(RetrySample("first input sample", 0, secondDate))
        retrySamplesSet.add(RetrySample("second input sample",  0, fourthDate))
        retrySamplesSet.add(RetrySample( "third input sample", 0, firstDate))


        Assertions.assertThat(retrySamplesSet.first().scheduledTime).isEqualTo(firstDate)
        Assertions.assertThat(retrySamplesSet.elementAt(1).scheduledTime).isEqualTo(secondDate)
        Assertions.assertThat(retrySamplesSet.last().scheduledTime).isEqualTo(fourthDate)

        retrySamplesSet.pollLast()

        retrySamplesSet.add(RetrySample( "fifth input sample", 0, thirdDate))


        Assertions.assertThat(retrySamplesSet.first().scheduledTime).isEqualTo(firstDate)
        Assertions.assertThat(retrySamplesSet.elementAt(1).scheduledTime).isEqualTo(secondDate)
        Assertions.assertThat(retrySamplesSet.last().scheduledTime).isEqualTo(thirdDate)
    }
}