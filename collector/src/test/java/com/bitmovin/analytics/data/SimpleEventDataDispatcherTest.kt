package com.bitmovin.analytics.data

import android.content.Context
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.dtos.AdEventData
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.AdType
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.license.AuthenticationResponse
import com.bitmovin.analytics.license.LicenseCall
import com.bitmovin.analytics.license.LicenseCallback
import com.bitmovin.analytics.utils.TestScopeProvider
import com.bitmovin.analytics.utils.areScopesCancelled
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class SimpleEventDataDispatcherTest {
    private val context = mockk<Context>()
    private val backendFactory = mockk<BackendFactory>()
    private val backend = mockk<Backend>()
    private val licenseCall = mockk<LicenseCall>()
    private val licenseCallback = mockk<LicenseCallback>(relaxed = true)
    private lateinit var scopeProvider: TestScopeProvider
    private lateinit var config: AnalyticsConfig
    private lateinit var dispatcher: SimpleEventDataDispatcher

    @Before
    fun setup() {
        config = AnalyticsConfig("blub")
        every { backendFactory.createBackend(config, context, any()) } returns backend
        scopeProvider = TestScopeProvider()
        dispatcher =
            SimpleEventDataDispatcher(
                context,
                config,
                licenseCallback,
                backendFactory,
                licenseCall,
                scopeProvider,
            )
    }

    @After
    fun teardown() {
        clearMocks(context, backendFactory, licenseCall, licenseCallback)
    }

    @Test
    fun `enabling the dispatcher authenticates against the license`() {
        dispatcher.enable()
        coVerify { licenseCall.authenticate(dispatcher) }
    }

    @Test
    fun `enabling the dispatcher creates a new backend`() {
        dispatcher.enable()
        verify { backendFactory.createBackend(config, context, any()) }
    }

    @Test
    fun `disabling the dispatcher cancels the scope`() {
        dispatcher.enable()
        dispatcher.disable()
        assert(scopeProvider.areScopesCancelled)
    }

    @Test
    fun `adding data before successful authentication does not forward data to the backend`() {
        dispatcher.enable()
        dispatcher.add(createTestEventData())
        verify(exactly = 0) { backend.send(any()) }
    }

    @Test
    fun `adding ad data before successful authentication does not forward data to the backend`() {
        dispatcher.enable()
        dispatcher.addAd(createTestAdEventData())
        verify(exactly = 0) { backend.sendAd(any()) }
    }

    @Test
    fun `successful authentication flushes cued data to the backend`() {
        dispatcher.enable()
        dispatcher.add(createTestEventData())
        verify(exactly = 0) { backend.send(any()) }

        dispatcher.authenticationCompleted(
            AuthenticationResponse.Granted("authenticated-key", null),
        )

        verify(exactly = 1) { backend.send(match { it.key == "authenticated-key" }) }
    }

    @Test
    fun `successful authentication flushes cued ad data to the backend`() {
        dispatcher.enable()
        dispatcher.addAd(createTestAdEventData())
        verify(exactly = 0) { backend.sendAd(any()) }

        dispatcher.authenticationCompleted(
            AuthenticationResponse.Granted("authenticated-key", null),
        )

        verify(exactly = 1) { backend.sendAd(match { it.key == "authenticated-key" }) }
    }

    @Test
    fun `data queued before detach is sent when the license is granted after detach`() {
        dispatcher.enable()
        dispatcher.add(createTestEventData())
        dispatcher.addAd(createTestAdEventData())
        dispatcher.disable()

        dispatcher.authenticationCompleted(
            AuthenticationResponse.Granted("authenticated-key", null),
        )

        verify(exactly = 1) { backend.send(match { it.key == "authenticated-key" }) }
        verify(exactly = 1) { backend.sendAd(match { it.key == "authenticated-key" }) }
    }

    @Test
    fun `data queued before detach is dropped when the license is denied after detach`() {
        dispatcher.enable()
        dispatcher.add(createTestEventData())
        dispatcher.disable()

        dispatcher.authenticationCompleted(AuthenticationResponse.Denied("denied"))

        verify(exactly = 0) { backend.send(any()) }
    }

    @Test
    fun `detaching with queued data keeps the scope until the license call completes`() {
        dispatcher.enable()
        dispatcher.add(createTestEventData())
        dispatcher.disable()

        assertThat(scopeProvider.areScopesCancelled).isFalse

        dispatcher.authenticationCompleted(AuthenticationResponse.Error)

        assertThat(scopeProvider.areScopesCancelled).isTrue
    }

    @Test
    fun `a license response after detach does not notify the collector`() {
        dispatcher.enable()
        dispatcher.add(createTestEventData())
        dispatcher.disable()

        dispatcher.authenticationCompleted(
            AuthenticationResponse.Granted("authenticated-key", null),
        )

        verify(exactly = 0) { licenseCallback.configureFeatures(any(), any()) }
        verify(exactly = 0) { licenseCallback.authenticationCompleted(any()) }
    }

    @Test
    fun `a license granted after detach does not authenticate the next attach`() {
        dispatcher.enable()
        dispatcher.disable()
        dispatcher.authenticationCompleted(
            AuthenticationResponse.Granted("authenticated-key", null),
        )

        dispatcher.enable()
        dispatcher.add(createTestEventData())

        verify(exactly = 0) { backend.send(any()) }
    }
}

private fun createTestAdEventData(key: String? = null) =
    AdEventData.fromEventData(
        createTestEventData(key),
        AdType.CLIENT_SIDE,
    )

private fun createTestEventData(key: String? = null) =
    EventData(
        DeviceInformation(
            manufacturer = "manufacturer",
            model = "model",
            isTV = true,
            locale = "locale",
            domain = "domain",
            screenHeight = 1,
            screenWidth = 1,
            operatingSystem = "operatingSystem",
            operatingSystemMajor = "operatingSystemMajor",
            operatingSystemMinor = "operatingSystemMinor",
        ),
        PlayerInfo(playerTech = "test", playerType = PlayerType.BITMOVIN),
        CustomData(),
        impressionId = "impressionId",
        userId = "userId",
        key = key,
        videoId = "videoId",
        videoTitle = "videoTitle",
        customUserId = "customUserId",
        path = "path",
        cdnProvider = "cdnProvider",
        userAgent = "userAgent",
    )
