package com.bitmovin.analytics

import android.app.Activity
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BitmovinAnalyticsTest {

    private lateinit var bitmovinAnalyticsConfig: BitmovinAnalyticsConfig

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("<ANALYTICS_KEY>")
        bitmovinAnalyticsConfig.customData1 = "customData1"
        bitmovinAnalyticsConfig.customData2 = "customData2"
        bitmovinAnalyticsConfig.customData3 = "customData3"
        bitmovinAnalyticsConfig.customData4 = "customData4"
        bitmovinAnalyticsConfig.customData5 = "customData5"
        bitmovinAnalyticsConfig.customData6 = "customData6"
        bitmovinAnalyticsConfig.customData7 = "customData7"

        mockkConstructor(BackendFactory::class)
        every { anyConstructed<BackendFactory>().createBackend(any(), any()) } returns mockk(relaxed = true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDeprecatedConstructorChecksForNullInConfiguration() {
        val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("foo-bar")
        BitmovinAnalytics(bitmovinAnalyticsConfig)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNewDefaultConstructorChecksForNull() {
        val bitmovinAnalyticsConfig = BitmovinAnalyticsConfig("foo-bar")
        BitmovinAnalytics(bitmovinAnalyticsConfig, null)
    }

    @Test
    fun testDetachPlayerShouldCallOnAnalyticsReleasingEventListener() {
        val listener = mockk<OnAnalyticsReleasingEventListener>(relaxed = true)
        val analytics = BitmovinAnalytics(bitmovinAnalyticsConfig, Activity())
        analytics.onAnalyticsReleasingObservable.subscribe(listener)
        analytics.detachPlayer()
        verify(exactly = 1) { listener.onReleasing() }
    }

    @Test
    fun testOnVideoStartFailedShouldCallOnErrorDetailEventListener() {
        val listener = mockk<OnErrorDetailEventListener>(relaxed = true)
        mockkConstructor(PlayerStateMachine::class)
        val analytics = spyk(BitmovinAnalytics(bitmovinAnalyticsConfig, Activity()))
        every { analytics.sendEventData(any()) } answers {}
        every { analytics.createEventData() }.returns(mockk(relaxed = true))
        every { analytics.playerStateMachine.videoStartFailedReason } returns VideoStartFailedReason.TIMEOUT
        analytics.onErrorDetailObservable.subscribe(listener)
        analytics.onVideoStartFailed()
        verify(exactly = 1) { listener.onError(any(), VideoStartFailedReason.TIMEOUT.errorCode?.errorCode, VideoStartFailedReason.TIMEOUT.errorCode?.description, any()) }
    }
}
