package com.bitmovin.analytics.data.testutils

import android.os.Handler
import android.os.Looper
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.adapters.PlayerContext
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.PlayerInfo
import com.bitmovin.analytics.dtos.EventData
import com.bitmovin.analytics.dtos.FeatureConfigContainer
import com.bitmovin.analytics.enums.PlayerType
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.ssai.SsaiEngagementMetricsService
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID

internal fun createTestImpressionId(numberOfImpression: Int = 1) = UUID(0xB177E57, numberOfImpression.toLong()).toString()

internal class DummyPlayerAdapter(
    analytics: BitmovinAnalytics,
    playerContext: PlayerContext,
) : PlayerAdapter {
    override val stateMachine: PlayerStateMachine =
        PlayerStateMachine.Factory.create(
            analytics,
            playerContext,
            Looper.getMainLooper(),
            deviceInformationProvider = DeviceInformationProvider(analytics.context),
        )
    override val isAutoplayEnabled: Boolean?
        get() = null

    override val drmDownloadTime: Long
        get() = 0

    private val ssaiEngagementMetricsService: SsaiEngagementMetricsService =
        SsaiEngagementMetricsService(
            analytics,
            AnalyticsConfig("dummy", ssaiEngagementTrackingEnabled = true),
            this,
            Handler(Looper.getMainLooper()),
        )

    override val ssaiService: SsaiService = SsaiService(stateMachine, ssaiEngagementMetricsService)

    override var defaultMetadata: DefaultMetadata = DefaultMetadata()

    override val playerInfo: PlayerInfo
        get() = PlayerInfo("Android:Testing", PlayerType.EXOPLAYER)

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        return emptyList()
    }

    override fun getCurrentSourceMetadata(): SourceMetadata = SourceMetadata()

    override fun release() {
    }

    override fun resetSourceRelatedState() {
    }

    override fun createEventData(): EventData {
        return TestFactory.createEventData(createTestImpressionId(1001))
    }

    override fun createEventDataForAdSample(): EventData {
        return TestFactory.createEventData(createTestImpressionId(1))
    }

    override fun createEventDataForCustomDataEvent(sourceMetadata: SourceMetadata): EventData {
        return TestFactory.createEventData(createTestImpressionId(1))
    }

    override fun triggerLastSampleOfSession() {}
}

internal class DummyPlayerContext : PlayerContext {
    override fun isPlaying(): Boolean {
        return false
    }

    override val position: Long
        get() = 0
}

internal fun createDummyPlayerAdapter(bitmovinAnalytics: BitmovinAnalytics): DummyPlayerAdapter =
    runBlocking {
        withContext(CoroutineScope(Dispatchers.Main).coroutineContext) {
            // Can not be created on the test thread
            DummyPlayerAdapter(
                bitmovinAnalytics,
                DummyPlayerContext(),
            )
        }
    }
