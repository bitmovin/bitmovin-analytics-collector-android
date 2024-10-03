package com.bitmovin.analytics.data

import com.bitmovin.analytics.utils.Util

/**
 * This class is used to hold both the impression_id and sequence_number and to make
 * sure both are reset atomically when a new session is started.
 *
 */
class SequenceNumberAndImpressionIdProvider {
    private var sampleSequenceNumber: Int = 0
    private var impressionId = Util.uUID

    @Synchronized
    fun getAndIncrementSequenceNumber(): Int {
        val temp = sampleSequenceNumber
        sampleSequenceNumber++
        return temp
    }

    @Synchronized
    fun getImpressionId(): String {
        return impressionId
    }

    @Synchronized
    fun reset() {
        sampleSequenceNumber = 0
        impressionId = Util.uUID
    }
}
