package com.bitmovin.analytics

import android.content.Context
import android.net.Uri
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.data.BackendFactory
import com.bitmovin.analytics.enums.VideoStartFailedReason
import com.bitmovin.analytics.features.errordetails.OnErrorDetailEventListener
import com.bitmovin.analytics.persistence.queue.AnalyticsEventQueue
import com.bitmovin.analytics.stateMachines.DefaultStateMachineListener
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BitmovinAnalyticsTest {

    private lateinit var analyticsConfig: AnalyticsConfig
    private lateinit var context: Context

    @Before
    fun setup() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk()
        MockitoAnnotations.openMocks(this)
        context = mockk {
            every { applicationContext } returns mockk()
        }
        analyticsConfig = AnalyticsConfig("<ANALYTICS_KEY>")
        mockkConstructor(BackendFactory::class)
        every { anyConstructed<BackendFactory>().createBackend(any(), any(), any()) } returns mockk(relaxed = true)
    }

    @Test
    fun testDetachPlayerShouldCallOnAnalyticsReleasingEventListener() {
        val listener = mockk<OnAnalyticsReleasingEventListener>(relaxed = true)
        val eventQueue = mockk<AnalyticsEventQueue>(relaxed = true)
        val analytics = BitmovinAnalytics(analyticsConfig, context, eventQueue)
        analytics.onAnalyticsReleasingObservable.subscribe(listener)
        analytics.detachPlayer()
        verify(exactly = 1) { listener.onReleasing() }
    }

    @Test
    fun testOnVideoStartFailedShouldCallOnErrorDetailEventListener() {
        val listener = mockk<OnErrorDetailEventListener>(relaxed = true)
        mockkConstructor(PlayerStateMachine::class)
        val eventQueue = mockk<AnalyticsEventQueue>(relaxed = true)
        val analytics = BitmovinAnalytics(analyticsConfig, context, eventQueue)
        val observable = ObservableSupport<OnErrorDetailEventListener>()
        val defaultStateMachineListener = DefaultStateMachineListener(analytics, mockk(relaxed = true), observable)
        val stateMachine = mockk<PlayerStateMachine>()
        val impressionId = "randomImpressionId"
        every { stateMachine.videoStartFailedReason } returns VideoStartFailedReason.TIMEOUT
        every { stateMachine.impressionId } returns impressionId
        observable.subscribe(listener)
        defaultStateMachineListener.onVideoStartFailed(stateMachine)
        verify(exactly = 1) { listener.onError(impressionId, VideoStartFailedReason.TIMEOUT.errorCode?.errorCode, VideoStartFailedReason.TIMEOUT.errorCode?.description, any()) }
    }
}
