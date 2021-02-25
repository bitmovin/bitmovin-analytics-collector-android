package com.bitmovin.analytics

import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EventBusTests {
    interface TestEvent {
        fun onTest()
    }

    interface TestEvent1 {
        fun onTest()
    }

    @Test
    fun testShouldReturnObservable() {
        val eventBus = EventBus()
        val observable = eventBus[TestEvent::class]
        assertThat(observable).isNotNull
    }

    @Test
    fun testShouldReturnSameObservableOnMultipleCalls() {
        val eventBus = EventBus()
        val observable = eventBus[TestEvent::class]
        val observable1 = eventBus[TestEvent::class]
        assertThat(observable).isEqualTo(observable1)
    }

    @Test
    fun testShouldReturnDifferentObservablesForDifferentEvents() {
        val eventBus = EventBus()
        val observable = eventBus[TestEvent::class]
        val observable1 = eventBus[TestEvent1::class]
        assertThat(observable).isNotEqualTo(observable1)
    }

    @Test
    fun testShouldNotifyCorrectSubscribers() {
        val eventBus = EventBus()
        val testEvent = mockk<TestEvent>(relaxed = true)
        val testEvent1 = mockk<TestEvent1>(relaxed = true)
        eventBus[TestEvent::class].subscribe(testEvent)
        eventBus[TestEvent1::class].subscribe(testEvent1)
        eventBus.notify(TestEvent::class) { it.onTest() }
        verify { testEvent.onTest() }
        verify(exactly = 0) { testEvent1.onTest() }
    }

    @Test
    fun testShouldNotifyMultipleSubscribers() {
        val eventBus = EventBus()
        val testEvent = mockk<TestEvent>(relaxed = true)
        eventBus[TestEvent::class].subscribe(testEvent)
        eventBus[TestEvent::class].subscribe(testEvent)
        eventBus.notify(TestEvent::class) { it.onTest() }
        verify(exactly = 2){testEvent.onTest()}
    }

    @Test
    fun testShouldSubscribeAndNotifyWithGenericSupport() {
        val eventBus = EventBus()
        val listener = mockk<TestEvent>(relaxed = true)
        eventBus.subscribe(listener)
        eventBus.notify<TestEvent> { it.onTest() }
        verify(exactly = 1) { listener.onTest() }
        eventBus.unsubscribe(listener)
        eventBus.notify<TestEvent> { it.onTest() }
        verify(exactly = 1) { listener.onTest() }
    }
}
