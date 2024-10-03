package com.bitmovin.analytics.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.RetryPolicy
import com.bitmovin.analytics.data.persistence.EventDatabase
import com.bitmovin.analytics.data.testutils.TestFactory
import com.bitmovin.analytics.data.testutils.createDummyPlayerAdapter
import com.bitmovin.analytics.data.testutils.createTestImpressionId
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import com.bitmovin.analytics.systemtest.utils.RepeatRule
import com.bitmovin.analytics.systemtest.utils.combineByImpressionId
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoRetryTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dummyPlayerAdapter: PlayerAdapter
    private lateinit var config: AnalyticsConfig
    private lateinit var bitmovinAnalytics: BitmovinAnalytics

    @Rule
    @JvmField
    val repeatRule = RepeatRule()

    @Before
    fun setup() {
        val mockedIngressUrl = MockedIngress.startServer()
        MockedIngress.liveServerForwarding = false
        config =
            AnalyticsConfig(
                licenseKey = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0",
                retryPolicy = RetryPolicy.NO_RETRY,
                backendUrl = mockedIngressUrl,
            )

        bitmovinAnalytics =
            BitmovinAnalytics(
                config = config,
                context = appContext,
            )
        dummyPlayerAdapter = createDummyPlayerAdapter(bitmovinAnalytics)
    }

    @After
    fun tearDown() {
        MockedIngress.stopServer()
        bitmovinAnalytics.detachPlayer()
        EventDatabase.getInstance(appContext).purge()
        MockedIngress.liveServerForwarding = true
    }

    @Test
    fun test_send_more_than_sequence_number_limit() {
        bitmovinAnalytics.attach(dummyPlayerAdapter)
        val sessionId = createTestImpressionId()

        // Create more than the sequence number limit
        // Note: we can't write our own sequence number since it's written on the data sending
        val eventData =
            MutableList(SEQUENCE_NUMBER_LIMIT + 10) {
                TestFactory.createEventData(impressionId = sessionId)
            }

        // Send all events
        eventData.forEach {
            bitmovinAnalytics.sendEventData(it)
        }

        val impressionList = MockedIngress.waitForRequestsAndExtractImpressions().combineByImpressionId()
        assertThat(impressionList).hasSize(1)
        val impression = impressionList.values.first()
        assertThat(impression.eventDataList)
            .hasSize(SEQUENCE_NUMBER_LIMIT + 1)
            .isEqualTo(eventData.subList(0, SEQUENCE_NUMBER_LIMIT + 1))
    }
}
