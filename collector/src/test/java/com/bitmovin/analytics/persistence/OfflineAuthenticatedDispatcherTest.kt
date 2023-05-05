@file:OptIn(ExperimentalCoroutinesApi::class)

package com.bitmovin.analytics.persistence

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.TestFactory.createAdEventData
import com.bitmovin.analytics.TestFactory.createEventData
import com.bitmovin.analytics.data.Backend
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.data.EventData
import com.bitmovin.analytics.license.AuthenticationCallback
import com.bitmovin.analytics.license.AuthenticationResponse
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.license.LicenseCall
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.utils.TestScopeProvider
import com.bitmovin.analytics.utils.areScopesCancelled
import io.mockk.called
import io.mockk.clearMocks
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

class OfflineAuthenticatedDispatcherTest {
    private val outerLicenseCallback: LicenseCallback = mockk(relaxed = true)
    private val backendFactory: BackendFactory = mockk()
    private val backend: Backend = mockk()
    private val licenseCall: LicenseCall = mockk()
    private val analyticsEventQueue: AnalyticsEventQueue = mockk()
    private lateinit var scopeProvider: TestScopeProvider
    private lateinit var offlineAuthenticatedDispatcher: OfflineAuthenticatedDispatcher

    @Before
    fun setup() {
        scopeProvider = TestScopeProvider()
        every { backendFactory.createBackend(any(), any(), any()) } returns backend

        offlineAuthenticatedDispatcher = OfflineAuthenticatedDispatcher(
            mockk(),
            BitmovinAnalyticsConfig(),
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
        offlineAuthenticatedDispatcher.add(eventData)

        verify { analyticsEventQueue.push(eventData) }
    }

    @Test
    fun `adding an AdEventData when the dispatcher is unauthenticated it adds the event to the analytics queue`() {
        val eventData = createAdEventData()
        offlineAuthenticatedDispatcher.addAd(eventData)

        verify { analyticsEventQueue.push(eventData) }
    }

    @Test
    fun `adding an EventData when the dispatcher is unauthenticated it requests authentication`() {
        val eventData = createEventData()
        offlineAuthenticatedDispatcher.add(eventData)

        verify { licenseCall.authenticate(any()) }
    }

    @Test
    fun `adding an AdEventData when the dispatcher is unauthenticated it requests authentication`() {
        val adEventData = createAdEventData()
        offlineAuthenticatedDispatcher.addAd(adEventData)

        verify { licenseCall.authenticate(any()) }
    }

    @Test
    fun `adding an EventData when the dispatcher is authenticated it delegates the event to the backend`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(null),
        )

        val eventData = createEventData()
        offlineAuthenticatedDispatcher.add(eventData)

        verify { backend.send(eventData) }
    }

    @Test
    fun `adding an AdEventData when the dispatcher is authenticated it delegates the event to the backend`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(null),
        )

        val adEventData = createAdEventData()
        offlineAuthenticatedDispatcher.addAd(adEventData)

        verify { backend.sendAd(adEventData) }
    }

    @Test
    fun `adding multiple EventData when the dispatcher is authenticated it increments the sequence number`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(null),
        )
        offlineAuthenticatedDispatcher.resetSourceRelatedState()
        val eventData = listOf(
            createEventData(),
            createEventData(),
            createEventData(),
            createEventData(),
        )

        eventData.forEach { offlineAuthenticatedDispatcher.add(it) }

        val eventSlots = mutableListOf<EventData>()
        verify(exactly = eventData.size) { backend.send(capture(eventSlots)) }
        eventSlots.forEachIndexed { index, event ->
            assertThat(event.sequenceNumber).isEqualTo(index)
        }
    }

    @Test
    fun `resetting source related state resets the sequence number for events`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(null),
        )
        offlineAuthenticatedDispatcher.resetSourceRelatedState()
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

        eventDataOfFirstSource.forEach { offlineAuthenticatedDispatcher.add(it) }
        offlineAuthenticatedDispatcher.resetSourceRelatedState()
        eventDataOfSecondSource.forEach { offlineAuthenticatedDispatcher.add(it) }

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
        offlineAuthenticatedDispatcher.disable()
        eventDataOfFirstSource.forEach { offlineAuthenticatedDispatcher.add(it) }

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
        offlineAuthenticatedDispatcher.disable()
        adEventDataOfFirstSource.forEach { offlineAuthenticatedDispatcher.addAd(it) }

        verify { backend wasNot called }
        verify { analyticsEventQueue wasNot called }
        verify { licenseCall wasNot called }
    }

    @Test
    fun `disabling the dispatcher resets the sequence number for events`() {
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(null),
        )
        offlineAuthenticatedDispatcher.resetSourceRelatedState()
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

        eventDataOfFirstSource.forEach { offlineAuthenticatedDispatcher.add(it) }
        offlineAuthenticatedDispatcher.disable()
        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(null),
        )
        eventDataOfSecondSource.forEach { offlineAuthenticatedDispatcher.add(it) }

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
        offlineAuthenticatedDispatcher.disable()

        assertThat(scopeProvider.areScopesCancelled).isTrue
    }

    @Test
    fun `enabling the dispatcher after it was disabled recreates scope and backend`() {
        offlineAuthenticatedDispatcher.disable()
        offlineAuthenticatedDispatcher.enable()

        assertThat(scopeProvider.areScopesCancelled).isFalse
        verify(exactly = 2) { backendFactory.createBackend(any(), any(), any()) }
    }

    @Test
    fun `receiving a granting licensing response it calls the license callback`() {
        val featureConfigContainer = FeatureConfigContainer(null)

        triggerAuthenticationCallback(
            AuthenticationResponse.Granted(featureConfigContainer),
        )

        verifyOrder {
            outerLicenseCallback.configureFeatures(true, featureConfigContainer)
            outerLicenseCallback.authenticationCompleted(true)
        }
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
        eventDataOfFirstSource.forEach { offlineAuthenticatedDispatcher.add(it) }
        adEventDataOfFirstSource.forEach { offlineAuthenticatedDispatcher.addAd(it) }

        verify { backend wasNot called }
        verify(exactly = 1) { analyticsEventQueue.push(any<EventData>()) }
        verify(exactly = 1) { licenseCall.authenticate(any()) }
    }

    @Test
    fun `receiving an error licensing response does not call the license callback`() {
        triggerAuthenticationCallback(AuthenticationResponse.Error)

        verify(exactly = 0) {
            outerLicenseCallback.configureFeatures(any(), any())
            outerLicenseCallback.authenticationCompleted(any())
        }
    }

    private fun triggerAuthenticationCallback(response: AuthenticationResponse) {
        val eventData = createEventData()
        offlineAuthenticatedDispatcher.add(eventData)
        val authenticationCallbackSlot = slot<AuthenticationCallback>()
        verify { licenseCall.authenticate(capture(authenticationCallbackSlot)) }

        authenticationCallbackSlot.captured.authenticationCompleted(
            response,
        )
    }
}
