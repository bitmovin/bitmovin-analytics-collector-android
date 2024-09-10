package com.bitmovin.analytics.adapters

import android.os.Handler
import android.os.Looper
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.manipulators.EventDataManipulator
import com.bitmovin.analytics.features.Feature
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.license.FeatureConfigContainer
import com.bitmovin.analytics.ssai.SsaiApiProxy
import com.bitmovin.analytics.ssai.SsaiEngagementMetricsService
import com.bitmovin.analytics.ssai.SsaiService
import com.bitmovin.analytics.stateMachines.PlayerStateMachine

abstract class DefaultPlayerAdapter(
    protected val config: AnalyticsConfig,
    protected val eventDataFactory: EventDataFactory,
    final override val stateMachine: PlayerStateMachine,
    private val featureFactory: FeatureFactory,
    private val deviceInformationProvider: DeviceInformationProvider,
    protected val metadataProvider: MetadataProvider,
    bitmovinAnalytics: BitmovinAnalytics,
    ssaiApiProxy: SsaiApiProxy,
    looper: Looper,
) : PlayerAdapter {
    protected abstract val eventDataManipulators: Collection<EventDataManipulator>

    // TODO [AN-4317]: this wiring is not good, we should aim for getting rid of the PlayerAdapter dependency
    private val ssaiEngagementMetricsService: SsaiEngagementMetricsService =
        SsaiEngagementMetricsService(analytics = bitmovinAnalytics, playerAdapter = this, Handler(looper))
    final override val ssaiService = SsaiService(stateMachine, ssaiEngagementMetricsService)

    override val isAutoplayEnabled: Boolean? = null

    init {
        eventDataFactory.registerEventDataManipulator(ssaiService)
        ssaiApiProxy.attach(ssaiService)
    }

    override fun init(): Collection<Feature<FeatureConfigContainer, *>> {
        eventDataManipulators.forEach { eventDataFactory.registerEventDataManipulator(it) }
        return featureFactory.createFeatures()
    }

    override fun createEventData() =
        eventDataFactory.create(
            stateMachine.impressionId,
            getCurrentSourceMetadata(),
            defaultMetadata,
            deviceInformationProvider.getDeviceInformation(),
            playerInfo,
            ssaiService.adMetadata,
        )

    // this method is used to create eventData samples which are used within
    // ad samples, this offers a way to have slightly different logic
    // for eventData that is used within ad samples
    // (mainly used for ssai to have a custom manipulator)
    override fun createEventDataForAdSample() =
        eventDataFactory.createForAdSamples(
            stateMachine.impressionId,
            getCurrentSourceMetadata(),
            defaultMetadata,
            deviceInformationProvider.getDeviceInformation(),
            playerInfo,
            ssaiService.adMetadata,
        )

    override fun triggerLastSampleOfSession() {
        stateMachine.triggerLastSampleOfSession()
    }

    override fun createEventDataForCustomDataEvent(sourceMetadata: SourceMetadata) =
        eventDataFactory.create(
            stateMachine.impressionId,
            sourceMetadata,
            defaultMetadata,
            deviceInformationProvider.getDeviceInformation(),
            playerInfo,
            ssaiService.adMetadata,
        )

    override fun release() {
        eventDataFactory.clearEventDataManipulators()
        stateMachine.release()
    }

    override var defaultMetadata: DefaultMetadata
        get() = metadataProvider.defaultMetadata
        set(value) {
            metadataProvider.defaultMetadata = value
        }

    override fun getCurrentSourceMetadata(): SourceMetadata {
        return metadataProvider.getSourceMetadata() ?: SourceMetadata()
    }
}
