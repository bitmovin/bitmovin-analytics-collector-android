package com.bitmovin.analytics.retryBackend

import com.bitmovin.analytics.retryBackend.RetrySample
import org.assertj.core.api.Assertions
import org.junit.Test
import java.util.*

class RetrySampleTest {

    @Test
    fun testSamplesShouldBeOrderedByScheduledTime() {
        // #region Mocking
        var retrySamplesSet = sortedSetOf<RetrySample>()
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
        // #endRegion

        retrySamplesSet.add(RetrySample(null, null,  0, fourthDate, 0))
        retrySamplesSet.add(RetrySample(null, null, 0, secondDate, 0))
        retrySamplesSet.add(RetrySample( null, null, 0, firstDate, 0))


        Assertions.assertThat(retrySamplesSet.first().scheduledTime).isEqualTo(firstDate)
        Assertions.assertThat(retrySamplesSet.elementAt(1).scheduledTime).isEqualTo(secondDate)
        Assertions.assertThat(retrySamplesSet.last().scheduledTime).isEqualTo(fourthDate)

        retrySamplesSet.pollLast()

        retrySamplesSet.add(RetrySample( null, null, 0, thirdDate, 0))


        Assertions.assertThat(retrySamplesSet.first().scheduledTime).isEqualTo(firstDate)
        Assertions.assertThat(retrySamplesSet.elementAt(1).scheduledTime).isEqualTo(secondDate)
        Assertions.assertThat(retrySamplesSet.last().scheduledTime).isEqualTo(thirdDate)
    }
}