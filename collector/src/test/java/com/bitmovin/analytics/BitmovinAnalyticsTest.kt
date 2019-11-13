package com.bitmovin.analytics

import android.test.mock.MockContext
import com.bitmovin.analytics.data.EventData
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BitmovinAnalyticsTest {

    @Mock
    private lateinit var context: MockContext


    private lateinit var bitmovinAnalyticsConfig: BitmovinAnalyticsConfig



    @Before
    fun setup(){
        MockitoAnnotations.initMocks(this)
        bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("<ANALYTICS_KEY>")
        bitmovinAnalyticsConfig.customData1 = "customData1"
        bitmovinAnalyticsConfig.customData2 = "customData2"
        bitmovinAnalyticsConfig.customData3 = "customData3"
        bitmovinAnalyticsConfig.customData4 = "customData4"
        bitmovinAnalyticsConfig.customData5 = "customData5"
        bitmovinAnalyticsConfig.customData6 = "customData6"
        bitmovinAnalyticsConfig.customData7 = "customData7"
    }


    @Test(expected = IllegalArgumentException::class)
    fun testDeprecatedConstructorChecksForNullInConfiguration() {
        val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("foo-bar")
        BitmovinAnalytics(bitmovinAnalyticsConfig)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNewDefaultConstructorChecksForNull() {
        val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("foo-bar")
        BitmovinAnalytics(bitmovinAnalyticsConfig, null)
    }
}