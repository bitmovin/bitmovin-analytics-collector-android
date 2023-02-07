package com.bitmovin.analytics.amazon.ivs.features

import com.bitmovin.analytics.features.errordetails.ErrorDetailTracking
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AmazonIvsPlayerFeatureFactoryTest {

    @Test
    fun createFeatures_ShouldAddErrorTracking() {
        // arrange
        val factory = AmazonIvsPlayerFeatureFactory(mockk(relaxed = true), mockk(relaxed = true))

        // act
        val features = factory.createFeatures()

        // assert
        assertThat(features).isNotEmpty
        assertThat(features.first()).isExactlyInstanceOf(ErrorDetailTracking::class.java)
    }
}
