package com.bitmovin.analytics

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ObservableSupportTests {
    interface TestEventListener {
        fun onEvent()
    }

    @Test
    fun testShouldSuccessfullyNotifySubscribers() {
        val observableSupport = ObservableSupport<TestEventListener>()
        val listener = mockk<TestEventListener>(relaxed = true)
        val listener1 = mockk<TestEventListener>(relaxed = true)
        observableSupport.subscribe(listener)
        observableSupport.subscribe(listener1)
        observableSupport.notify { it.onEvent() }
        verify(exactly = 1) { listener.onEvent() }
        verify(exactly = 1) { listener1.onEvent() }
    }

    @Test
    fun testShouldSuccessfullyNotifyMultipleTimes() {
        val observableSupport = ObservableSupport<TestEventListener>()
        val listener = mockk<TestEventListener>(relaxed = true)
        observableSupport.subscribe(listener)
        observableSupport.notify { it.onEvent() }
        observableSupport.notify { it.onEvent() }
        verify(exactly = 2) { listener.onEvent() }
    }

    @Test
    fun testShouldSuccessfullyUnsubscribe() {
        val observableSupport = ObservableSupport<TestEventListener>()
        val listener = mockk<TestEventListener>(relaxed = true)
        observableSupport.subscribe(listener)
        observableSupport.notify { it.onEvent() }
        observableSupport.unsubscribe(listener)
        observableSupport.notify { it.onEvent() }
        verify(exactly = 1) { listener.onEvent() }
    }
}
