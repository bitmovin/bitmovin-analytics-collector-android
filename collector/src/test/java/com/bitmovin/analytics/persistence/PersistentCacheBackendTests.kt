package com.bitmovin.analytics.persistence

import com.bitmovin.analytics.TestFactory
import com.bitmovin.analytics.data.CallbackBackend
import com.bitmovin.analytics.data.OnFailureCallback
import com.bitmovin.analytics.data.OnSuccessCallback
import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import io.mockk.Call
import io.mockk.MockKAnswerScope
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class PersistentCacheBackendTests {
    private lateinit var backend: PersistentCacheBackend

    private val callbackBackend = mockk<CallbackBackend>()
    private val eventQueue = mockk<AnalyticsEventQueue>()

    @Before
    fun setup() {
        backend = PersistentCacheBackend(callbackBackend, eventQueue)
    }

    @After
    fun cleanup() {
        clearMocks(callbackBackend, eventQueue)
    }

    @Test
    fun `calling send calls CallbackBackend_send`() {
        val event = TestFactory.createEventData()

        backend.send(event, null, null)

        verify { callbackBackend.send(event, any(), any()) }
    }

    @Test
    fun `calling sendAd calls CallbackBackend_sendAd`() {
        val event = TestFactory.createAdEventData()

        backend.sendAd(event, null, null)

        verify { callbackBackend.sendAd(event, any(), any()) }
    }

    @Test
    fun `when backend Event sending succeeds the OnSuccessCallback is called`() {
        testCallbackBackendCallbacks(TestFactory.createEventData(), success = true)
    }

    @Test
    fun `when backend Event sending fails the OnFailureCallback is called`() {
        testCallbackBackendCallbacks(TestFactory.createEventData(), success = false)
    }

    @Test
    fun `when backend AdEvent sending succeeds the OnSuccessCallback is called`() {
        testCallbackBackendCallbacks(TestFactory.createAdEventData(), success = true)
    }

    @Test
    fun `when backend AdEvent sending fails the OnFailureCallback is called`() {
        testCallbackBackendCallbacks(TestFactory.createAdEventData(), success = false)
    }

    @Test
    fun `when the Event sending fails the Event is pushed to the queue`() {
        val backend = PersistentCacheBackend(callbackBackend, eventQueue)
        val event = TestFactory.createEventData()
        every { callbackBackend.send(event, any(), any()) } answers {
            thirdArg<OnFailureCallback>().onFailure(Exception()) {}
        }

        backend.send(event, null, null)

        verify { eventQueue.push(event) }
    }

    @Test
    fun `when the AdEvent sending fails the AdEvent is pushed to the queue`() {
        val event = TestFactory.createAdEventData()
        every { callbackBackend.sendAd(event, any(), any()) } answers {
            thirdArg<OnFailureCallback>().onFailure(Exception()) {}
        }

        backend.sendAd(event, null, null)

        verify { eventQueue.push(event) }
    }

    private fun testCallbackBackendCallbacks(
        event: Any,
        success: Boolean,
    ) {
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

            else -> error("Unknown event type")
        }

        if (success) {
            assert(onSuccessCalled)
        } else {
            assert(onFailureCallbackCalled)
        }
    }
}
