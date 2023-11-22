@file:OptIn(ExperimentalCoroutinesApi::class)

package com.bitmovin.analytics.persistence

import com.bitmovin.analytics.TestFactory.createAdEventData
import com.bitmovin.analytics.TestFactory.createEventData
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.data.CacheConsumingBackend
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.license.AuthenticationCallback
import com.bitmovin.analytics.license.AuthenticationResponse
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.license.LicenseCall
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.utils.TestScopeProvider
import com.bitmovin.analytics.utils.areScopesCancelled
import io.mockk.Called
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

private const val TEST_LICENSE_KEY: String = "test_key"

class PersistingAuthenticatedDispatcherTest {
    private val outerLicenseCallback: LicenseCallback = mockk(relaxed = true)
    private val backendFactory: BackendFactory = mockk()
    private val backend: Backend = mockk(
        moreInterfaces = arrayOf(CacheConsumingBackend::class),
    )
    private val licenseCall: LicenseCall = mockk()
    private val analyticsEventQueue: AnalyticsEventQueue = mockk()
    private lateinit var scopeProvider: TestScopeProvider
    private lateinit var persistingAuthenticatedDispatcher: PersistingAuthenticatedDispatcher

    @Before
    fun setup() {
        scopeProvider = TestScopeProvider()
        every { backendFactory.createBackend(any(), any(), any()) } returns backend

        persistingAuthenticatedDispatcher = PersistingAuthenticatedDispatcher(
            mockk(),
            AnalyticsConfig(TEST_LICENSE_KEY),
            outerLicenseCallback,
            backendFactory,
            licenseCall,
            analyticsEventQueue,
            scopeProvider,
        )
    }

    @After
    fun clearState() {
        clearMocks(
            outerLicenseCallback,
            backendFactory,
            backend,
            licenseCall,
            analyticsEventQueue,
        )
    }

    @Test
    fun `adding an EventData when the dispatcher is unauthenticated it adds the event to the analytics queue`() {
        val eventData = createEventData()
        persistingAuthenticatedDispatcher.add(eventData)

        verify { analyticsEventQueue.push(eventData) }
    }

    @Test
    fun `adding an AdEventData when the dispatcher is unauthenticated it adds the event to the analytics queue`() {
        val eventData = createAdEventData()
        persistingAuthenticatedDispatcher.addAd(eventData)

        verify { analyticsEventQueue.push(eventData) }
    }

    @Test
    fun `adding an EventData when the dispatcher is unauthenticated it requests authentication`() {
        val eventData = createEventData()
        persistingAuthenticatedDispatcher.add(eventData)

        coVerify { licenseCall.authenticate(any()) }
    }

    @Test
    fun `adding an AdEventData when the dispatcher is unauthenticated it requests authentication`() {
        val adEventData = createAdEventData()
        persistingAuthenticatedDispatcher.addAd(adEventData)

        coVerify { licenseCall.authenticate(any()) }
    }

    @Test
    fun `adding an EventData when the dispatcher is authenticated it delegates the event to the backend`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(TEST_LICENSE_KEY, null),
        )

        val eventData = createEventData()
        persistingAuthenticatedDispatcher.add(eventData)

        verify { backend.send(eventData) }
    }

    @Test
    fun `adding an AdEventData when the dispatcher is authenticated it delegates the event to the backend`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(TEST_LICENSE_KEY, null),
        )

        val adEventData = createAdEventData()
        persistingAuthenticatedDispatcher.addAd(adEventData)

        verify { backend.sendAd(adEventData) }
    }

    @Test
    fun `adding multiple EventData when the dispatcher is authenticated it increments the sequence number`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(TEST_LICENSE_KEY, null),
        )
        persistingAuthenticatedDispatcher.resetSourceRelatedState()
        val eventData = listOf(
            createEventData(),
            createEventData(),
            createEventData(),
            createEventData(),
        )

        eventData.forEach { persistingAuthenticatedDispatcher.add(it) }

        val eventSlots = mutableListOf<EventData>()
        verify(exactly = eventData.size) { backend.send(capture(eventSlots)) }
        eventSlots.forEachIndexed { index, event ->
            assertThat(event.sequenceNumber).isEqualTo(index)
        }
    }

    @Test
    fun `resetting source related state resets the sequence number for events`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(TEST_LICENSE_KEY, null),
        )
        persistingAuthenticatedDispatcher.resetSourceRelatedState()
        val eventDataOfFirstSource = listOf(
            createEventData(),
            createEventData(),
            createEventData(),
            createEventData(),
        )
        val eventDataOfSecondSource = listOf(
            createEventData(),
            createEventData(),
            createEventData(),
            createEventData(),
        )

        eventDataOfFirstSource.forEach { persistingAuthenticatedDispatcher.add(it) }
        persistingAuthenticatedDispatcher.resetSourceRelatedState()
        eventDataOfSecondSource.forEach { persistingAuthenticatedDispatcher.add(it) }

        val eventSlots = mutableListOf<EventData>()
        verify(exactly = eventDataOfFirstSource.size + eventDataOfSecondSource.size) {
            backend.send(capture(eventSlots))
        }
        eventSlots.forEachIndexed { index, event ->
            assertThat(event.sequenceNumber).isEqualTo(index % 4)
        }
    }

    @Test
    fun `adding EventData after the dispatcher is disabled does nothing`() {
        val eventDataOfFirstSource = listOf(
            createEventData(),
            createEventData(),
            createEventData(),
            createEventData(),
        )
        persistingAuthenticatedDispatcher.disable()
        eventDataOfFirstSource.forEach { persistingAuthenticatedDispatcher.add(it) }

        verify { backend wasNot called }
        verify { analyticsEventQueue wasNot called }
        verify { licenseCall wasNot called }
    }

    @Test
    fun `adding AdEventData after the dispatcher is disabled does nothing`() {
        val adEventDataOfFirstSource = listOf(
            createAdEventData(),
            createAdEventData(),
            createAdEventData(),
            createAdEventData(),
        )
        persistingAuthenticatedDispatcher.disable()
        adEventDataOfFirstSource.forEach { persistingAuthenticatedDispatcher.addAd(it) }

        verify { backend wasNot called }
        verify { analyticsEventQueue wasNot called }
        verify { licenseCall wasNot called }
    }

    @Test
    fun `disabling the dispatcher resets the sequence number for events`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(TEST_LICENSE_KEY, null),
        )
        persistingAuthenticatedDispatcher.resetSourceRelatedState()
        val eventDataOfFirstSource = listOf(
            createEventData(),
            createEventData(),
            createEventData(),
            createEventData(),
        )
        val eventDataOfSecondSource = listOf(
            createEventData(),
            createEventData(),
            createEventData(),
            createEventData(),
        )

        eventDataOfFirstSource.forEach { persistingAuthenticatedDispatcher.add(it) }
        persistingAuthenticatedDispatcher.disable()
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(TEST_LICENSE_KEY, null),
        )
        eventDataOfSecondSource.forEach { persistingAuthenticatedDispatcher.add(it) }

        val eventSlots = mutableListOf<EventData>()
        verify(exactly = eventDataOfFirstSource.size + eventDataOfSecondSource.size) {
            backend.send(capture(eventSlots))
        }
        eventSlots.drop(eventDataOfFirstSource.size).forEachIndexed { index, event ->
            // Because of the triggerAuthenticationCallback, the sequence number is offset
            // by 1
            val expectedSequenceNumber = index + 1
            assertThat(event.sequenceNumber).isEqualTo(expectedSequenceNumber)
        }
    }

    @Test
    fun `disabling the dispatcher cancels all scopes`() {
        persistingAuthenticatedDispatcher.disable()

        assertThat(scopeProvider.areScopesCancelled).isTrue
    }

    @Test
    fun `enabling the dispatcher after it was disabled recreates scope and backend`() {
        persistingAuthenticatedDispatcher.disable()
        persistingAuthenticatedDispatcher.enable()

        assertThat(scopeProvider.areScopesCancelled).isFalse
        verify(exactly = 2) { backendFactory.createBackend(any(), any(), any()) }
    }

    @Test
    fun `receiving a granting licensing response it calls the license callback`() {
        val featureConfigContainer = FeatureConfigContainer(null)

        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(TEST_LICENSE_KEY, featureConfigContainer),
        )

        verifyOrder {
            outerLicenseCallback.configureFeatures(true, featureConfigContainer)
            outerLicenseCallback.authenticationCompleted(true)
        }
    }

    @Test
    fun `receiving a granting licensing response starts flushing the cache`() {
        triggerAuthenticationCallback(AuthenticationResponse.Granted(TEST_LICENSE_KEY, null))

        assertThat(backend).isInstanceOf(CacheConsumingBackend::class.java)
        verify(exactly = 1) { (backend as CacheConsumingBackend).startCacheFlushing() }
    }

    @Test
    fun `receiving a denying licensing response it calls the license callback`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Denied("Some Message"),
        )

        verifyOrder {
            outerLicenseCallback.configureFeatures(false, null)
            outerLicenseCallback.authenticationCompleted(false)
        }
    }

    @Test
    fun `receiving a denying licensing response clears the analytics event queue`() {
        val licenseMessage = "Some Message"
        triggerAuthenticationCallback(
            AuthenticationResponse.Denied(licenseMessage),
        )

        verify { analyticsEventQueue.clear() }
    }

    @Test
    fun `receiving a denying licensing response disables the dispatcher`() {
        val licenseMessage = "Some Message"
        triggerAuthenticationCallback(
            AuthenticationResponse.Denied(licenseMessage),
        )
        val eventDataOfFirstSource = listOf(
            createEventData(),
            createEventData(),
            createEventData(),
            createEventData(),
        )
        val adEventDataOfFirstSource = listOf(
            createAdEventData(),
            createAdEventData(),
            createAdEventData(),
            createAdEventData(),
        )
        eventDataOfFirstSource.forEach { persistingAuthenticatedDispatcher.add(it) }
        adEventDataOfFirstSource.forEach { persistingAuthenticatedDispatcher.addAd(it) }

        verify { backend wasNot called }
        verify(exactly = 1) { analyticsEventQueue.push(any<EventData>()) }
        coVerify(exactly = 1) { licenseCall.authenticate(any()) }
    }

    @Test
    fun `receiving an error licensing response does not call the license callback`() {
        triggerAuthenticationCallback(AuthenticationResponse.Error)

        verify(exactly = 0) {
            outerLicenseCallback.configureFeatures(any(), any())
            outerLicenseCallback.authenticationCompleted(any())
        }
    }

    @Test
    fun `receiving an error licensing response does not flush the cache`() {
        triggerAuthenticationCallback(AuthenticationResponse.Error)

        assertThat(backend).isInstanceOf(CacheConsumingBackend::class.java)
        verify(exactly = 0) { backend wasNot Called }
    }

    private fun triggerAuthenticationCallback(response: AuthenticationResponse) {
        val eventData = createEventData()
        persistingAuthenticatedDispatcher.add(eventData)
        val authenticationCallbackSlot = slot<AuthenticationCallback>()
        coVerify { licenseCall.authenticate(capture(authenticationCallbackSlot)) }

        authenticationCallbackSlot.captured.authenticationCompleted(
            response,
        )
    }
}
