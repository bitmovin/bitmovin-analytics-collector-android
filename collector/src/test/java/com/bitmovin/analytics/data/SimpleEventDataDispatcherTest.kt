@file:OptIn(ExperimentalCoroutinesApi::class)

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
import com.bitmovin.analytics.utils.TestScopeProvider
import com.bitmovin.analytics.utils.areScopesCancelled
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class SimpleEventDataDispatcherTest {
    private val context = mockk<Context>()
    private val backendFactory = mockk<BackendFactory>()
    private val backend = mockk<Backend>()
    private val licenseCall = mockk<LicenseCall>()
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
                null,
                backendFactory,
                licenseCall,
                scopeProvider,
            )
    }

    @After
    fun teardown() {
        clearMocks(context, backendFactory, licenseCall)
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

    // FIXME: why do we need the scope in the constructor?
    @Test
    @Ignore("This test fails as the scope created in the constructor is not cancelled. This is a bug.")
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
