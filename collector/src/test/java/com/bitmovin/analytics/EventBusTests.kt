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
        var event1Called = false
        var event2Called = false
        eventBus[TestEvent::class].subscribe(object : TestEvent {
            override fun onTest() {
                event1Called = true
            }
        })
        eventBus[TestEvent1::class].subscribe(object : TestEvent1 {
            override fun onTest() {
                event2Called = true
            }
        })
        eventBus.notify(TestEvent::class) { it.onTest() }
        assertThat(event1Called).isTrue()
        assertThat(event2Called).isFalse()
    }

    @Test
    fun testShouldNotifyMultipleSubscribers() {
        val eventBus = EventBus()
        var count = 0
        eventBus[TestEvent::class].subscribe(object : TestEvent {
            override fun onTest() {
                count++
            }
        })
        eventBus[TestEvent::class].subscribe(object : TestEvent {
            override fun onTest() {
                count++
            }
        })
        eventBus.notify(TestEvent::class) { it.onTest() }
        assertThat(count).isEqualTo(2)
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
