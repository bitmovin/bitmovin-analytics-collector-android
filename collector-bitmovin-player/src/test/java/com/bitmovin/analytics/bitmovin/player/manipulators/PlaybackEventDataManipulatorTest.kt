package com.bitmovin.analytics.bitmovin.player.manipulators

import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.BitmovinSdkAdapter
import com.bitmovin.analytics.bitmovin.player.player.LatencyMeter
import com.bitmovin.analytics.bitmovin.player.player.PlaybackQualityProvider
import com.bitmovin.analytics.bitmovin.player.player.PlayerLicenseProvider
import com.bitmovin.analytics.data.DeviceInformation
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.stateMachines.PlayerStates
import com.bitmovin.analytics.utils.DownloadSpeedMeter
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.Source
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.util.UUID

class PlaybackEventDataManipulatorTest {
    @RelaxedMockK
    private lateinit var player: Player

    @RelaxedMockK
    private lateinit var adapter: BitmovinSdkAdapter

    @RelaxedMockK
    private lateinit var playerContext: PlayerContext

    @RelaxedMockK
    private lateinit var playbackQualityProvider: PlaybackQualityProvider

    @RelaxedMockK
    private lateinit var playerLicenseProvider: PlayerLicenseProvider

    @RelaxedMockK
    private lateinit var downloadSpeedMeter: DownloadSpeedMeter

    private val latencyMeter = LatencyMeter()

    private lateinit var manipulator: PlaybackEventDataManipulator

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        manipulator =
            PlaybackEventDataManipulator(
                player = player,
                playerContext = playerContext,
                adapter = adapter,
                playbackQualityProvider = playbackQualityProvider,
                playerLicenseProvider = playerLicenseProvider,
                downloadSpeedMeter = downloadSpeedMeter,
                latencyMeter = latencyMeter,
            )
    }

    @Test
    fun `manipulate sets isLive to true from player given no sourceMetadata set`() {
        // arrange
        val currentSource: Source = mockkSource(duration = Double.POSITIVE_INFINITY)
        every { adapter.currentSource } returns currentSource
        every { adapter.getCurrentSourceMetadata() } returns SourceMetadata()
        val eventData = createTestEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
        assertThat(eventData.videoDuration).isEqualTo(0)
    }

    @Test
    fun `manipulate sets isLive to false from player given no sourceMetadata set`() {
        // arrange
        val currentSource: Source = mockkSource(duration = 1234.0)
        every { adapter.currentSource } returns currentSource
        every { adapter.getCurrentSourceMetadata() } returns SourceMetadata()
        val eventData = createTestEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isFalse
        assertThat(eventData.videoDuration).isEqualTo(1234000L)
    }

    @Test
    fun `manipulate sets isLive to true from sourceMetadata`() {
        // arrange
        val currentSource: Source = mockkSource(duration = 1234.0)
        every { adapter.currentSource } returns currentSource
        every { adapter.getCurrentSourceMetadata() } returns SourceMetadata(isLive = true)
        val eventData = createTestEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.isLive).isTrue
        assertThat(eventData.videoDuration).isEqualTo(1234000L)
    }

    @Test
    fun `manipulate sets latencyInfo given live stream in playing state`() {
        // arrange
        every { adapter.currentSource } returns mockkSource(duration = Double.POSITIVE_INFINITY)
        every { adapter.getCurrentSourceMetadata() } returns SourceMetadata()
        every { adapter.stateMachine.currentState } returns PlayerStates.PLAYING
        latencyMeter.addMeasurement(4.0, 3.0)
        latencyMeter.addMeasurement(5.0, 3.0)
        val eventData = createTestEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.latencyInfo).isNotNull
        assertThat(eventData.latencyInfo!!.avgLatency).isEqualTo(4500L)
        assertThat(eventData.latencyInfo!!.targetLatency).isEqualTo(3000L)
        assertThat(eventData.latencyInfo!!.avgLatencyDelta).isEqualTo(1500L)
    }

    @Test
    fun `manipulate resets latency meter after playing sample`() {
        // arrange
        every { adapter.currentSource } returns mockkSource(duration = Double.POSITIVE_INFINITY)
        every { adapter.getCurrentSourceMetadata() } returns SourceMetadata()
        every { adapter.stateMachine.currentState } returns PlayerStates.PLAYING
        latencyMeter.addMeasurement(4.0, 3.0)

        // act
        manipulator.manipulate(createTestEventData())
        val secondEventData = createTestEventData()
        manipulator.manipulate(secondEventData)

        // assert
        assertThat(secondEventData.latencyInfo).isNull()
    }

    @Test
    fun `manipulate does not set latencyInfo given no latency measurements`() {
        // arrange
        every { adapter.currentSource } returns mockkSource(duration = Double.POSITIVE_INFINITY)
        every { adapter.getCurrentSourceMetadata() } returns SourceMetadata()
        every { adapter.stateMachine.currentState } returns PlayerStates.PLAYING
        val eventData = createTestEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.latencyInfo).isNull()
    }

    @Test
    fun `manipulate does not set latencyInfo and resets meter given not in playing state`() {
        // arrange
        every { adapter.currentSource } returns mockkSource(duration = Double.POSITIVE_INFINITY)
        every { adapter.getCurrentSourceMetadata() } returns SourceMetadata()
        every { adapter.stateMachine.currentState } returns PlayerStates.PAUSE
        latencyMeter.addMeasurement(4.0, 3.0)
        val eventData = createTestEventData()

        // act
        manipulator.manipulate(eventData)

        // assert
        assertThat(eventData.latencyInfo).isNull()
        // measurements collected before the pause sample must not leak into the next playing sample
        assertThat(latencyMeter.getInfoAndReset()).isNull()
    }

    private fun mockkSource(duration: Double): Source =
        mockk(relaxed = true) {
            every { this@mockk.duration } returns duration
        }

    private fun createTestEventData(): EventData {
        val analyticsConfig = AnalyticsConfig(licenseKey = "licenseKey")
        return EventDataFactory(
            analyticsConfig,
            mockk(relaxed = true),
            mockk(relaxed = true),
        ).create(
            UUID.randomUUID().toString(),
            SourceMetadata(),
            DefaultMetadata(),
            DeviceInformation("myManufacturer", "myModel", false, "de", "package-name", 100, 200),
            PlayerInfo("Android:Bitmovin", PlayerType.BITMOVIN),
            null,
        )
    }
}
