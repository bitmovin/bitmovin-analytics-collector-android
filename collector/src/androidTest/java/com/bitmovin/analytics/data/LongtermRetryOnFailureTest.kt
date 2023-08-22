package com.bitmovin.analytics.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.RetryPolicy
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.persistence.EventDatabase
import com.bitmovin.analytics.data.persistence.PersistentAnalyticsEventQueue
import com.bitmovin.analytics.data.testutils.TestFactory
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.persistence.EventQueueConfig
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.systemtest.utils.Impression
import com.bitmovin.analytics.systemtest.utils.MockedIngress
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class LongtermRetryOnFailureTest {
    private val mainScope = MainScope()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dummyPlayerAdapter: PlayerAdapter
    private lateinit var config: AnalyticsConfig
    private lateinit var bitmovinAnalytics: BitmovinAnalytics

    @Before
    fun setup() {
        val mockedIngressUrl = MockedIngress.startServer()
        config = AnalyticsConfig(licenseKey = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0", retryPolicy = RetryPolicy.LONG_TERM, backendUrl = mockedIngressUrl)

        bitmovinAnalytics = BitmovinAnalytics(
            config = config,
            context = appContext,
        )
        dummyPlayerAdapter = createDummyPlayerAdapter()
    }

    private fun createDummyPlayerAdapter(): DummyPlayerAdapter = runBlocking {
        withContext(mainScope.coroutineContext) {
            // Can not be created on the test thread
            DummyPlayerAdapter(
                bitmovinAnalytics,
                DummyPlayerContext(),
            )
        }
    }

    @After
    fun tearDown() {
        MockedIngress.stopServer()
        bitmovinAnalytics.detachPlayer()
        EventDatabase.getInstance(appContext).purge()
    }

    @Test
    fun test_sending_eventdata_sends_the_events() {
        bitmovinAnalytics.attach(dummyPlayerAdapter)
        val sessionId = createTestImpressionId()
        val eventData = MutableList(5) {
            TestFactory.createEventData(impressionId = sessionId)
        }

        eventData.forEach {
            bitmovinAnalytics.sendEventData(it)

            // waiting a bit here after each call, to make it more likely that the license
            // authentication call is already done
            Thread.sleep(400)
        }

        Thread.sleep(500)

        val impressionList = MockedIngress.extractImpressions().combineByImpressionId()
        assertThat(impressionList).hasSize(1)
        assertThat(impressionList.values.first().eventDataList)
            .hasSize(5)
            .isEqualTo(eventData)
    }

    @Test
    fun test_online_offline_online_sends_all_eventdata() {
        // this test uses a hardcoded port, since we cannot go online/offline/online due to
        // the fact that the server socket cannot be reused if port 0 is used (if there is traffic as it seems)
        val mockedIngressUrl = MockedIngress.startServer(61123)
        config = AnalyticsConfig(licenseKey = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0", retryPolicy = RetryPolicy.LONG_TERM, backendUrl = mockedIngressUrl)

        bitmovinAnalytics = BitmovinAnalytics(
            config = config,
            context = appContext,
        )
        dummyPlayerAdapter = createDummyPlayerAdapter()
        bitmovinAnalytics.attach(dummyPlayerAdapter)
        val sessionId = createTestImpressionId()
        val eventData = MutableList(2) {
            TestFactory.createEventData(impressionId = sessionId)
        }

        eventData.forEach {
            bitmovinAnalytics.sendEventData(it)

            // waiting a bit here after each call, to make it more likely that the license
            // authentication call is already done
            Thread.sleep(400)
        }

        Thread.sleep(500)

        val impressionsBeforeOffline = MockedIngress.extractImpressions().combineByImpressionId()
        assertThat(impressionsBeforeOffline).hasSize(1)
        assertThat(impressionsBeforeOffline.values.first().eventDataList)
            .hasSize(2)

        MockedIngress.setServerOffline()

        eventData.forEach {
            bitmovinAnalytics.sendEventData(it)

            // waiting a bit here after each call, to make it more likely that the license
            // authentication call is already done
            Thread.sleep(400)
        }

        Thread.sleep(500)
        // server didn't receive any new requests since it is offline
        assertThat(MockedIngress.requestCount()).isEqualTo(3)

        MockedIngress.setServerOnline()

        eventData.forEach {
            bitmovinAnalytics.sendEventData(it)

            // waiting a bit here after each call, to make it more likely that the license
            // authentication call is already done
            Thread.sleep(400)
        }

        // all samples in queue are sent together with the new samples
        val impressionsAfterOffline = MockedIngress.extractImpressions().combineByImpressionId()
        assertThat(impressionsAfterOffline).hasSize(1)
        assertThat(impressionsAfterOffline.values.first().eventDataList)
            .hasSize(4)
    }

    @Test
    fun test_sending_ad_eventdata_sends_the_adevents() {
        bitmovinAnalytics.attach(dummyPlayerAdapter)
        val sessionId = createTestImpressionId()

        val eventData: List<AdEventData> = MutableList(5) {
            TestFactory.createAdEventData(
                adImpressionId = sessionId,
                videoImpressionId = sessionId,
            )
        }

        eventData.forEach {
            bitmovinAnalytics.sendAdEventData(it)

            // waiting a bit here after each call, to make it more likely that the license
            // authentication call is already done
            Thread.sleep(400)
        }
        Thread.sleep(500)

        val impressions = MockedIngress.extractImpressions().combineByImpressionId()
        assertThat(impressions).hasSize(1)
        assertThat(impressions.values.first().adEventDataList)
            .hasSize(5)
            .isEqualTo(eventData)
    }

    @Test
    fun test_failed_ad_and_eventdata_sending_they_are_send_with_the_next_successful_one() {
        MockedIngress.setServerOffline()
        val cachedSessionId = createTestImpressionId()
        val sessionId = createTestImpressionId(2)
        bitmovinAnalytics.attach(dummyPlayerAdapter)

        val eventData = MutableList(5) {
            TestFactory.createEventData(impressionId = cachedSessionId)
        }

        val adEventData: List<AdEventData> = MutableList(5) {
            TestFactory.createAdEventData(
                adImpressionId = cachedSessionId,
                videoImpressionId = cachedSessionId,
            )
        }

        eventData.forEach { bitmovinAnalytics.sendEventData(it) }
        adEventData.forEach { bitmovinAnalytics.sendAdEventData(it) }
        Thread.sleep(1000)

        assertThat(MockedIngress.hasNoSamplesReceived()).isTrue

        // activate ingress backend again
        MockedIngress.setServerOnline()
        bitmovinAnalytics.attach(dummyPlayerAdapter)

        // Trigger sending of cached data
        bitmovinAnalytics.sendEventData(TestFactory.createEventData(impressionId = sessionId))
        Thread.sleep(5000)

        val impressions = MockedIngress.extractImpressions().combineByImpressionId()
        assertThat(impressions).hasSize(2)
        assertThat(impressions[cachedSessionId]!!.eventDataList)
            .hasSize(5)
        assertThat(impressions[cachedSessionId]!!.adEventDataList)
            .hasSize(5)
    }

    @Test
    fun test_failed_ad_and_eventdata_sending_are_send_by_another_bitmovin_analytics_instance() {
        val cachedSessionId = createTestImpressionId()
        val sessionId = createTestImpressionId(2)
        val offlineConfig = config.copy(backendUrl = "https://doesnotwork")

        bitmovinAnalytics = BitmovinAnalytics(
            config = offlineConfig,
            context = appContext,
        )

        bitmovinAnalytics.attach(dummyPlayerAdapter)

        val eventData = MutableList(5) {
            TestFactory.createEventData(impressionId = cachedSessionId)
        }

        val adEventData: List<AdEventData> = MutableList(5) {
            TestFactory.createAdEventData(
                adImpressionId = cachedSessionId,
                videoImpressionId = cachedSessionId,
            )
        }

        eventData.forEach { bitmovinAnalytics.sendEventData(it) }
        adEventData.forEach { bitmovinAnalytics.sendAdEventData(it) }
        Thread.sleep(1000)
        bitmovinAnalytics.detachPlayer()

        val secondInstance = BitmovinAnalytics(
            config = config,
            context = appContext,
        )
        dummyPlayerAdapter = createDummyPlayerAdapter()
        secondInstance.attach(dummyPlayerAdapter)

        // Trigger sending of cached data
        secondInstance.sendEventData(TestFactory.createEventData(impressionId = sessionId))
        Thread.sleep(5000)

        val impressions = MockedIngress.extractImpressions().combineByImpressionId()
        assertThat(impressions).hasSize(2)
        val cachedImpression = impressions[cachedSessionId]
        val triggerImpression = impressions[sessionId]
        assertThat(cachedImpression!!.eventDataList).hasSize(5)
        assertThat(cachedImpression.adEventDataList).hasSize(5)
        assertThat(triggerImpression!!.eventDataList).hasSize(1)
    }

    @Test
    fun test_having_multiple_bitmovin_analytics_instance_events_are_send_once() {
        val offlineConfig = AnalyticsConfig(licenseKey = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0", retryPolicy = RetryPolicy.LONG_TERM, backendUrl = "https://doesnotwork")
        val firstSession = createTestImpressionId()
        val secondSession = createTestImpressionId(2)

        val firstBitmovinAnalytics = BitmovinAnalytics(
            config = offlineConfig,
            context = appContext,
        )

        val secondBitmovinAnalytics = BitmovinAnalytics(
            config = offlineConfig,
            context = appContext,
        )
        val firstDummyPlayerAdapter = createDummyPlayerAdapter()
        firstBitmovinAnalytics.attach(firstDummyPlayerAdapter)

        val secondDummyPlayerAdapter = createDummyPlayerAdapter()
        secondBitmovinAnalytics.attach(secondDummyPlayerAdapter)

        val eventDataFirstCollector = MutableList(100) {
            TestFactory.createEventData(impressionId = firstSession)
        }

        val eventDataSecondCollector = MutableList(100) {
            TestFactory.createEventData(impressionId = secondSession)
        }

        val firstJob = mainScope.launch {
            eventDataFirstCollector.forEach { firstBitmovinAnalytics.sendEventData(it) }
        }
        val secondJob = mainScope.launch {
            eventDataSecondCollector.forEach { secondBitmovinAnalytics.sendEventData(it) }
        }
        runBlocking {
            firstJob.join()
            secondJob.join()
        }
        Thread.sleep(1000)

        val onlineConfig = AnalyticsConfig(licenseKey = "17e6ea02-cb5a-407f-9d6b-9400358fbcc0", retryPolicy = RetryPolicy.LONG_TERM)
        val thirdBitmovinAnalytics = BitmovinAnalytics(
            config = onlineConfig,
            context = appContext,
        )
        val forthBitmovinAnalytics = BitmovinAnalytics(
            config = onlineConfig,
            context = appContext,
        )

        thirdBitmovinAnalytics.attach(firstDummyPlayerAdapter)
        forthBitmovinAnalytics.attach(secondDummyPlayerAdapter)

        // Trigger sending of cached data
        thirdBitmovinAnalytics.sendEventData(TestFactory.createEventData(impressionId = firstSession))
        forthBitmovinAnalytics.sendEventData(TestFactory.createEventData(impressionId = secondSession))

        Thread.sleep(40000)

        val impressions = MockedIngress.extractImpressions().combineByImpressionId()

        impressions.values.forEach {
            assertThat(it.eventDataList).hasSize(101)
        }
    }

    @Test
    fun test_having_more_than_14_days_old_events_in_the_event_queue_they_are_not_send_later() {
        bitmovinAnalytics.attach(dummyPlayerAdapter)
        val oldSession1 = createTestImpressionId(1)
        val oldSession2 = createTestImpressionId(2)
        val newSession = createTestImpressionId(1337)

        val oldTimestamp = (System.currentTimeMillis().milliseconds - 15.days).inWholeMilliseconds

        val oldEventData = MutableList(5) {
            TestFactory.createEventData(
                impressionId = oldSession1,
                time = oldTimestamp + it,
            )
        }.apply {
            TestFactory.createEventData(
                impressionId = oldSession1,
                time = System.currentTimeMillis(),
            )
            repeat(5) {
                add(
                    TestFactory.createEventData(
                        impressionId = oldSession2,
                        time = oldTimestamp + it,
                    ),
                )
            }
        }
        val oldAdEventData = MutableList(5) {
            TestFactory.createAdEventData(
                videoImpressionId = oldSession1,
                adImpressionId = oldSession1,
                time = oldTimestamp + it,
            )
        }.apply {
            repeat(5) {
                add(
                    TestFactory.createAdEventData(
                        videoImpressionId = oldSession2,
                        adImpressionId = oldSession2,
                        time = oldTimestamp + it,
                    ),
                )
            }
        }

        PersistentAnalyticsEventQueue(
            EventQueueConfig(),
            EventDatabase.getInstance(appContext),
        ).run {
            oldAdEventData.forEach(::push)
            oldEventData.forEach(::push)
        }

        // Trigger sending of cached data
        bitmovinAnalytics.sendEventData(TestFactory.createEventData(impressionId = newSession))

        Thread.sleep(1000)

        val impressions = MockedIngress.extractImpressions().combineByImpressionId()
        assertThat(impressions.values).hasSize(1)
        assertThat(impressions.values.first().eventDataList).hasSize(1)
    }

    @Test
    fun test_reaching_the_overall_count_limit_deletes_the_oldest_session() {
        config = config.copy(backendUrl = "https://doesnotwork")
        bitmovinAnalytics = BitmovinAnalytics(
            config = config,
            context = appContext,
        )

        bitmovinAnalytics.attach(dummyPlayerAdapter)

        val newSession = createTestImpressionId()

        val persistentQueue = PersistentAnalyticsEventQueue(
            EventQueueConfig(),
            EventDatabase.getInstance(appContext),
        )
        val sessionIds = persistentQueue.almostFillDatabaseToCountLimit()

        assertThat(persistentQueue.popEvent()!!.impressionId).isEqualTo(sessionIds.first())

        repeat(150) {
            bitmovinAnalytics.sendEventData(TestFactory.createEventData(impressionId = newSession))
        }
        Thread.sleep(2000)
        val secondSessionId = sessionIds[1]

        assertThat(persistentQueue.popEvent()!!.impressionId).isEqualTo(secondSessionId)
        assertThat(persistentQueue.popAdEvent()!!.videoImpressionId).isEqualTo(secondSessionId)
    }

    @Test
    fun test_events_with_sequencenumber_higher_than_500_are_not_stored() {
        val offlineConfig = config.copy(backendUrl = "https://doesnotwork")
        bitmovinAnalytics = BitmovinAnalytics(
            config = offlineConfig,
            context = appContext,
        )
        dummyPlayerAdapter = createDummyPlayerAdapter()

        val persistentQueue = PersistentAnalyticsEventQueue(
            EventQueueConfig(),
            EventDatabase.getInstance(appContext),
        )

        val sessionId = UUID.randomUUID().toString()
        repeat(300) {
            bitmovinAnalytics.sendEventData(TestFactory.createEventData(impressionId = sessionId))
            bitmovinAnalytics.sendAdEventData(
                TestFactory.createAdEventData(
                    videoImpressionId = sessionId,
                    adImpressionId = sessionId,
                ),
            )
        }
        // go beyond the sequence number limit
        repeat(300) {
            bitmovinAnalytics.sendEventData(TestFactory.createEventData(impressionId = sessionId))
        }
        // try to send/store more ad event data
        repeat(300) {
            bitmovinAnalytics.sendAdEventData(
                TestFactory.createAdEventData(
                    videoImpressionId = sessionId,
                    adImpressionId = sessionId,
                ),
            )
        }

        bitmovinAnalytics.detachPlayer()

        var eventCount = 0
        var element = persistentQueue.popEvent()
        while (element != null) {
            eventCount++
            assertThat(element.sequenceNumber).isLessThan(501)
            element = persistentQueue.popEvent()
        }
        assertThat(eventCount).isEqualTo(501)

        var adEventCount = 0
        var adElement = persistentQueue.popAdEvent()
        while (adElement != null) {
            adEventCount++
            adElement = persistentQueue.popAdEvent()
        }
        assertThat(adEventCount).isEqualTo(300)
    }
}

private fun List<Impression>.combineByImpressionId(): Map<String, Impression> {
    val events = flatMap { it.eventDataList }.groupBy { it.impressionId }
    val adEvents = flatMap { it.adEventDataList }.groupBy { it.videoImpressionId }

    return (events.keys + adEvents.keys).associateWith {
        Impression(
            events[it]?.toMutableList() ?: mutableListOf(),
            adEvents[it]?.toMutableList() ?: mutableListOf(),
        )
    }
}

private fun createTestImpressionId(
    numberOfImpression: Int = 1,
) = UUID(0xB177E57, numberOfImpression.toLong()).toString()

private fun PersistentAnalyticsEventQueue.almostFillDatabaseToCountLimit(): List<String> {
    val startTime = (System.currentTimeMillis().milliseconds - 10.hours).inWholeMilliseconds
    val sessionIds = MutableList(10) { createTestImpressionId(it + 1) }

    sessionIds.take(9).forEachIndexed { index, sessionId ->
        repeat(500) {
            push(
                TestFactory.createEventData(
                    impressionId = sessionId,
                    sequenceNumber = it,
                    time = startTime + 500 * index + it,
                ),
            )
        }
        repeat(100) {
            push(
                TestFactory.createAdEventData(
                    videoImpressionId = sessionId,
                    adImpressionId = sessionId,
                    time = startTime + 500 * index + it,
                ),
            )
        }
    }
    val lastSessionId = sessionIds.last()
    repeat(400) {
        push(
            TestFactory.createEventData(
                impressionId = lastSessionId,
                sequenceNumber = it,
                time = startTime + 500 * 10 + it,
            ),
        )
    }
    return sessionIds
}

private class DummyPlayerAdapter(
    analytics: BitmovinAnalytics,
    playerContext: PlayerContext,
) : PlayerAdapter {
    override val stateMachine: PlayerStateMachine = PlayerStateMachine.Factory.create(
        analytics,
        playerContext,
    )
    override val position: Long
        get() = 0
    override val drmDownloadTime: Long?
        get() = 0

    override var defaultMetadata: DefaultMetadata = DefaultMetadata()

    override val playerInfo: PlayerInfo
        get() = PlayerInfo("Android:Testing", PlayerType.EXOPLAYER)

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        return emptyList()
    }

    override fun getCurrentSourceMetadata(): SourceMetadata = SourceMetadata()

    override fun release() {
    }

    override fun resetSourceRelatedState() {
    }

    override fun clearValues() {
    }

    override fun createEventData(): EventData {
        return TestFactory.createEventData(createTestImpressionId(1001))
    }

    override fun createEventDataForCustomDataEvent(sourceMetadata: SourceMetadata): EventData {
        return TestFactory.createEventData(createTestImpressionId(1))
    }
}

private class DummyPlayerContext : PlayerContext {
    override fun isPlaying(): Boolean {
        return false
    }

    override val position: Long
        get() = 0
}
