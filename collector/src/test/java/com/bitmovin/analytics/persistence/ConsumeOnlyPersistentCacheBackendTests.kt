package com.bitmovin.analytics.persistence

import com.bitmovin.analytics.TestFactory
import com.bitmovin.analytics.data.AdEventData
import com.bitmovin.analytics.data.CallbackBackend
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.data.OnFailureCallback
import com.bitmovin.analytics.data.OnSuccessCallback
import com.bitmovin.analytics.persistence.queue.ConsumeOnlyAnalyticsEventQueue
import com.bitmovin.analytics.persistence.queue.InMemoryEventQueue
import io.mockk.Call
import io.mockk.MockKAnswerScope
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConsumeOnlyPersistentCacheBackendTests {
    private val testScope = TestScope()
    private lateinit var backend: ConsumeOnlyPersistentCacheBackend

    private val callbackBackend = mockk<CallbackBackend>()
    private val eventQueue = mockk<ConsumeOnlyAnalyticsEventQueue>()

    @Before
    fun setup() {
        backend = ConsumeOnlyPersistentCacheBackend(
            testScope,
            callbackBackend,
            eventQueue,
        )
    }

    @After
    fun cleanup() {
        clearMocks(callbackBackend, eventQueue)
    }

    @Test
    fun `when the queue is empty no additional events are sent`() {
        every { callbackBackend.send(any(), any(), any()) } answers {
            secondArg<OnSuccessCallback>().onSuccess()
        }
        every { callbackBackend.sendAd(any(), any(), any()) } answers {
            secondArg<OnSuccessCallback>().onSuccess()
        }
        every { eventQueue.popEvent() } returns null
        every { eventQueue.popAdEvent() } returns null

        val event = TestFactory.createEventData()
        backend.send(event, null, null)
        testScope.testScheduler.runCurrent()

        verify(exactly = 1) {
            callbackBackend.send(event, any(), any())
        }
    }

    @Test
    fun `when a new EventData is sent successfully all enqueued Events and AdEvents get sent`() {
        testAdditionalSendingHappens(TestFactory.createEventData(impressionId = "new-id"))
    }

    @Test
    fun `when a new AdEventData is sent successfully all enqueued Events and AdEvents get sent`() {
        testAdditionalSendingHappens(TestFactory.createAdEventData(adId = "newAdId"))
    }

    @Test
    fun `when EventData failed to be sent additional sending shouldn't happen`() {
        testNoAdditionalSendingHappens(TestFactory.createEventData(impressionId = "new-id"))
    }

    @Test
    fun `when AdEventData failed to be sent additional sending shouldn't happen`() {
        testNoAdditionalSendingHappens(TestFactory.createAdEventData(adId = "newAdId"))
    }

    @Test
    fun `when EventData sending succeeds the OnSuccessCallback is called`() {
        testCallbackBackendCallbacks(TestFactory.createEventData(), success = true)
    }

    @Test
    fun `when EventData sending fails the OnFailureCallback is called`() {
        testCallbackBackendCallbacks(TestFactory.createEventData(), success = false)
    }

    @Test
    fun `when AdEventData sending succeeds the OnSuccessCallback is called`() {
        testCallbackBackendCallbacks(TestFactory.createAdEventData(), success = true)
    }

    @Test
    fun `when AdEventData sending fails the OnFailureCallback is called`() {
        testCallbackBackendCallbacks(TestFactory.createAdEventData(), success = false)
    }

    @Test
    fun `sending an EventData sends it with the CallbackBackend`() {
        val event = TestFactory.createEventData()

        backend.send(event)

        verify {
            callbackBackend.send(event, any(), any())
        }
    }

    @Test
    fun `sending an AdEventData sends it with the CallbackBackend`() {
        val event = TestFactory.createAdEventData()

        backend.sendAd(event)

        verify {
            callbackBackend.sendAd(event, any(), any())
        }
    }

    @Test
    fun `receiving multiple EventData when a cache item is flushed does not flush more queue items`() {
        val cachedEvent = TestFactory.createEventData(impressionId = "test-event")
        val event = TestFactory.createEventData(impressionId = "event")
        val cachedEventsCount = 3
        val sendEvents = 5
        var poppedEvents = 0

        var firstCachedEventSuccessCallback: OnSuccessCallback? = null
        every { callbackBackend.send(any(), any(), any()) } answers {
            if (firstArg<EventData>() == cachedEvent &&
                firstCachedEventSuccessCallback == null
            ) {
                firstCachedEventSuccessCallback = secondArg<OnSuccessCallback>()
            } else {
                secondArg<OnSuccessCallback>().onSuccess()
            }
        }
        every { eventQueue.popAdEvent() } returns null
        every { eventQueue.popEvent() } answers {
            if (poppedEvents >= cachedEventsCount) {
                null
            } else {
                poppedEvents++
                cachedEvent
            }
        }

        repeat(sendEvents) {
            backend.send(event, null, null)
            testScope.testScheduler.advanceUntilIdle()
        }

        verify(exactly = sendEvents) { callbackBackend.send(event, any(), any()) }
        verify(exactly = 1) { callbackBackend.send(cachedEvent, any(), any()) }
        verify(exactly = 1) { eventQueue.popEvent() }
        firstCachedEventSuccessCallback!!.onSuccess()
        testScope.testScheduler.advanceUntilIdle()

        verify(exactly = cachedEventsCount) { callbackBackend.send(cachedEvent, any(), any()) }
        assertThat(poppedEvents).isEqualTo(cachedEventsCount)
        verify(exactly = cachedEventsCount + 1) { eventQueue.popEvent() }
    }

    @Test
    fun `starting flushing the cache consumes the elements in the event queue`() {
        var poppedEvents = 0
        val expectedSendEvents = 3
        every { eventQueue.popEvent() } answers {
            poppedEvents++
            if (poppedEvents <= expectedSendEvents) {
                TestFactory.createEventData(impressionId = poppedEvents.toString())
            } else {
                null
            }
        }
        every { callbackBackend.send(any(), any(), any()) } answers {
            secondArg<OnSuccessCallback>().onSuccess()
        }

        backend.startCacheFlushing()
        testScope.advanceUntilIdle()

        val events = mutableListOf<EventData>()
        verify(exactly = expectedSendEvents) { callbackBackend.send(capture(events), any(), any()) }
        verify(exactly = expectedSendEvents + 1) { eventQueue.popEvent() }
        assertThat(
            events.map { it.impressionId.toInt() },
        ).isEqualTo(
            (1..expectedSendEvents).toList(),
        )
    }

    private fun testAdditionalSendingHappens(newEvent: Any) {
        requireEventDataOrAdEventData(newEvent)

        val testQueue = InMemoryEventQueue()
        val enqueuedEvent = TestFactory.createEventData(impressionId = "enqueuedId")
        val enqueuedAdEvent = TestFactory.createAdEventData("enqueuedAdId")
        val enqueuedEventCount = 2
        val enqueuedAdEventCount = 2
        repeat(enqueuedEventCount) {
            testQueue.push(enqueuedEvent)
        }
        repeat(enqueuedAdEventCount) {
            testQueue.push(enqueuedAdEvent)
        }
        val backend = ConsumeOnlyPersistentCacheBackend(
            testScope,
            callbackBackend,
            testQueue,
        )
        every { callbackBackend.send(any(), any(), any()) } answers {
            secondArg<OnSuccessCallback>().onSuccess()
        }
        every { callbackBackend.sendAd(any(), any(), any()) } answers {
            secondArg<OnSuccessCallback>().onSuccess()
        }

        when (newEvent) {
            is EventData -> backend.send(newEvent, null, null)
            is AdEventData -> backend.sendAd(newEvent, null, null)
        }
        testScope.testScheduler.advanceUntilIdle()

        verify(exactly = 1) {
            when (newEvent) {
                is EventData -> callbackBackend.send(newEvent, any(), any())
                is AdEventData -> callbackBackend.sendAd(newEvent, any(), any())
            }
        }
        verify(exactly = enqueuedEventCount) {
            callbackBackend.send(enqueuedEvent, any(), any())
        }
        verify(exactly = enqueuedAdEventCount) {
            callbackBackend.sendAd(enqueuedAdEvent, any(), any())
        }
    }

    private fun testNoAdditionalSendingHappens(newEvent: Any) {
        requireEventDataOrAdEventData(newEvent)

        val testQueue = InMemoryEventQueue()
        val enqueuedEvent = TestFactory.createEventData()
        val enqueuedAdEvent = TestFactory.createAdEventData("enqueuedAdId")
        val enqueuedEventCount = 2
        val enqueuedAdEventCount = 2
        repeat(enqueuedEventCount) {
            testQueue.push(enqueuedEvent)
        }
        repeat(enqueuedAdEventCount) {
            testQueue.push(enqueuedAdEvent)
        }
        val backend = ConsumeOnlyPersistentCacheBackend(
            testScope,
            callbackBackend,
            testQueue,
        )
        // New event sending fails, the opposite event type sending could still succeed.
        every { callbackBackend.send(any(), any(), any()) } answers {
            when (newEvent) {
                is EventData -> thirdArg<OnFailureCallback>().onFailure(Exception()) {}
                is AdEventData -> secondArg<OnSuccessCallback>().onSuccess()
            }
        }
        // New event sending fails, the opposite event type sending could still succeed.
        every { callbackBackend.sendAd(any(), any(), any()) } answers {
            when (newEvent) {
                is EventData -> secondArg<OnSuccessCallback>().onSuccess()
                is AdEventData -> thirdArg<OnFailureCallback>().onFailure(Exception()) {}
            }
        }

        when (newEvent) {
            is EventData -> backend.send(newEvent, null, failure = { _, _ -> })
            is AdEventData -> backend.sendAd(newEvent, null, failure = { _, _ -> })
        }
        testScope.testScheduler.advanceUntilIdle()

        verify(exactly = 1) {
            when (newEvent) {
                is EventData -> callbackBackend.send(newEvent, any(), any())
                is AdEventData -> callbackBackend.sendAd(newEvent, any(), any())
            }
        }
        verify(exactly = 0) {
            callbackBackend.send(enqueuedEvent, any(), any())
        }
        verify(exactly = 0) {
            callbackBackend.sendAd(enqueuedAdEvent, any(), any())
        }
    }

    private fun testCallbackBackendCallbacks(event: Any, success: Boolean) {
        requireEventDataOrAdEventData(event)

        var onSuccessCalled = false
        var onFailureCallbackCalled = false
        val onSuccessCallback = OnSuccessCallback { onSuccessCalled = true }
        val onFailureCallback = OnFailureCallback { _, _ -> onFailureCallbackCalled = true }
        val callbackAnswer: (MockKAnswerScope<Unit, Unit>).(Call) -> Unit = {
            if (success) {
                secondArg<OnSuccessCallback>().onSuccess()
            } else {
                thirdArg<OnFailureCallback>().onFailure(Exception()) {}
            }
        }

        when (event) {
            is EventData -> {
                every { callbackBackend.send(event, any(), any()) } answers callbackAnswer
                backend.send(event, onSuccessCallback, onFailureCallback)
            }

            is AdEventData -> {
                every { callbackBackend.sendAd(event, any(), any()) } answers callbackAnswer
                backend.sendAd(event, onSuccessCallback, onFailureCallback)
            }
        }

        if (success) {
            assert(onSuccessCalled)
        } else {
            assert(onFailureCallbackCalled)
        }
    }

    private fun requireEventDataOrAdEventData(event: Any) {
        require(event is EventData || event is AdEventData) {
            "event must be either EventData or AdEventData"
        }
    }
}
