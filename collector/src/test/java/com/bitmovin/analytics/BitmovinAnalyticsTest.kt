package com.bitmovin.analytics

import android.content.Context
import android.net.Uri
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.EventDataDispatcherFactory
import com.bitmovin.analytics.data.IEventDataDispatcher
import com.bitmovin.analytics.data.SequenceNumberAndImpressionIdProvider
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.stateMachines.DefaultStateMachineListener
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BitmovinAnalyticsTest {
    private lateinit var analyticsConfig: AnalyticsConfig
    private lateinit var context: Context

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
        MockitoAnnotations.openMocks(this)
        context =
            mockk {
                every { applicationContext } returns mockk()
            }
        analyticsConfig = AnalyticsConfig("<ANALYTICS_KEY>")
        mockkConstructor(BackendFactory::class)
        every { anyConstructed<BackendFactory>().createBackend(any(), any(), any()) } returns mockk(relaxed = true)
    }

    @Test
    fun testDetachPlayerShouldCallOnAnalyticsReleasingEventListener() {
        val listener = mockk<OnAnalyticsReleasingEventListener>(relaxed = true)
        val analytics = BitmovinAnalytics(analyticsConfig, context)
        analytics.onAnalyticsReleasingObservable.subscribe(listener)
        analytics.detachPlayer()
        verify(exactly = 1) { listener.onReleasing() }
    }

    @Test
    fun testOnVideoStartFailedShouldCallOnErrorDetailEventListener() {
        val listener = mockk<OnErrorDetailEventListener>(relaxed = true)
        mockkConstructor(PlayerStateMachine::class)
        val mockImpressionIdProvider = mockk<SequenceNumberAndImpressionIdProvider>(relaxed = true)
        val analytics =
            BitmovinAnalytics(analyticsConfig, context, sequenceNumberAndImpressionIdProvider = mockImpressionIdProvider)
        val observable = ObservableSupport<OnErrorDetailEventListener>()
        val ssaiService = mockk<SsaiService>(relaxed = true)
        val defaultStateMachineListener = DefaultStateMachineListener(analytics, mockk(relaxed = true), observable, ssaiService)
        val stateMachine = mockk<PlayerStateMachine>()
        val impressionId = "randomImpressionId"
        every { stateMachine.videoStartFailedReason } returns VideoStartFailedReason.TIMEOUT
        every { mockImpressionIdProvider.getImpressionId() } returns impressionId
        observable.subscribe(listener)
        defaultStateMachineListener.onVideoStartFailed(stateMachine)
        verify(exactly = 1) {
            listener.onError(
                impressionId,
                VideoStartFailedReason.TIMEOUT.errorCode?.errorCode,
                VideoStartFailedReason.TIMEOUT.errorCode?.description,
                any(),
            )
        }
    }

    @Test
    fun `sendEventData increases the sequence number and adds the eventData to the dispatcher`() {
        // arrange
        val mockDispatcherFactory = mockk<EventDataDispatcherFactory>(relaxed = true)
        val mockEventDataDispatcher = mockk<IEventDataDispatcher>(relaxed = true)
        every { mockDispatcherFactory.create(any()) } returns mockEventDataDispatcher

        val eventSlots = mutableListOf<EventData>()
        every { mockEventDataDispatcher.add(capture(eventSlots)) } answers { }

        val analytics = BitmovinAnalytics(analyticsConfig, context, eventDataDispatcherFactory = mockDispatcherFactory)

        // act
        analytics.sendEventData(TestFactory.createEventData())
        analytics.sendEventData(TestFactory.createEventData())
        analytics.sendEventData(TestFactory.createEventData())

        // assert
        verify(exactly = 3) { mockEventDataDispatcher.add(any()) }

        eventSlots.forEachIndexed { index, eventData ->
            assertThat(eventData.sequenceNumber).isEqualTo(index)
        }
    }

    @Test
    fun `sequence number is limited to the limit`() {
        // arrange
        val mockDispatcherFactory = mockk<EventDataDispatcherFactory>(relaxed = true)
        val mockEventDataDispatcher = mockk<IEventDataDispatcher>(relaxed = true)
        every { mockDispatcherFactory.create(any()) } returns mockEventDataDispatcher

        val eventSlots = mutableListOf<EventData>()
        every { mockEventDataDispatcher.add(capture(eventSlots)) } answers { }

        val analytics = BitmovinAnalytics(analyticsConfig, context, eventDataDispatcherFactory = mockDispatcherFactory)
        val eventData = TestFactory.createEventData()

        // act
        for (i in 0..1005) {
            analytics.sendEventData(eventData)
        }

        // assert
        // this line might cause issues when there are too many calls (verification would throw an error)
        // test is stalling then. in case of the correct amount of calls, test passes
        verify(exactly = 1001) { mockEventDataDispatcher.add(any()) }
    }
}
