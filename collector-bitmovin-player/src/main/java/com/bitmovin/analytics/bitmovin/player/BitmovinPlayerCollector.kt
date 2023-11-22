package com.bitmovin.analytics.bitmovin.player

import android.content.Context
import android.os.Handler
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector
import com.bitmovin.analytics.bitmovin.player.features.BitmovinFeatureFactory
import com.bitmovin.analytics.bitmovin.player.player.BitmovinPlayerContext
import com.bitmovin.analytics.bitmovin.player.player.PlaybackQualityProvider
import com.bitmovin.analytics.bitmovin.player.player.PlayerLicenseProvider
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.ApiV3Utils
import com.bitmovin.analytics.utils.SystemInformationProvider
import com.bitmovin.analytics.utils.UserAgentProvider
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.Source

@Deprecated(
    "Please use {@link IBitmovinCollector.Factory instead} instead.",
    replaceWith = ReplaceWith(
        expression = "IBitmovinPlayerCollector.Factory.create(context, analyticsConfig)",
        imports = ["com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector"],
    ),
)
class BitmovinPlayerCollector(analyticsConfig: AnalyticsConfig, context: Context) :
    DefaultCollector<Player>(analyticsConfig, context.applicationContext),
    IBitmovinPlayerCollector {
    private val deferredLicenseManager = DeferredLicenseRelay(analyticsConfig.licenseKey)

    override val analytics: BitmovinAnalytics by lazy {
        BitmovinAnalytics(
            config = analyticsConfig,
            context = context,
            licenseKeyProvider = deferredLicenseManager.licenseKeyProvider,
        )
    }

    override fun attachPlayer(player: Player) {
        deferredLicenseManager.attach(player)
        super.attachPlayer(player)
    }

    override fun detachPlayer() {
        deferredLicenseManager.detach()
        super.detachPlayer()
    }

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
     * @param context [Context]
     */
    @Deprecated(
        "Please use {@link IBitmovinCollector.Factory instead} instead.",
        replaceWith = ReplaceWith(
            expression = "IBitmovinPlayerCollector.Factory.create(context, analyticsConfig)",
            imports = ["com.bitmovin.analytics.bitmovin.player.api.IBitmovinPlayerCollector"],
        ),
    )
    constructor(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig, context: Context) : this(
        ApiV3Utils.extractAnalyticsConfig(bitmovinAnalyticsConfig),
        context,
    ) {
        this.setDeprecatedBitmovinAnalyticsConfig(bitmovinAnalyticsConfig)
    }

    override fun createAdapter(
        player: Player,
        analytics: BitmovinAnalytics,
    ): PlayerAdapter {
        val licenseKeyProvider = deferredLicenseManager.licenseKeyProvider
        val featureFactory: FeatureFactory = BitmovinFeatureFactory(
            analytics,
            player,
            licenseKeyProvider,
        )
        val userAgentProvider = UserAgentProvider(
            Util.getApplicationInfoOrNull(analytics.context),
            Util.getPackageInfoOrNull(analytics.context),
            SystemInformationProvider.getProperty("http.agent"),
        )
        val eventDataFactory = EventDataFactory(
            config,
            userIdProvider,
            userAgentProvider,
            licenseKeyProvider,
        )
        val deviceInformationProvider = DeviceInformationProvider(analytics.context)
        val playerLicenseProvider = PlayerLicenseProvider(analytics.context)
        val playerContext = BitmovinPlayerContext(player)
        val handler = Handler(analytics.context.mainLooper)
        val stateMachine = PlayerStateMachine.Factory.create(analytics, playerContext, handler)
        val playbackQualityProvider = PlaybackQualityProvider(player)
        return BitmovinSdkAdapter(
            player,
            config,
            stateMachine,
            featureFactory,
            eventDataFactory,
            deviceInformationProvider,
            playerLicenseProvider,
            playbackQualityProvider,
            metadataProvider,
        )
    }

    @Deprecated(
        "Use setSourceMetadata instead",
        ReplaceWith("setSourceMetadata(sourceMetadata, playerSource)"),
    )
    override fun addSourceMetadata(playerSource: Source, sourceMetadata: SourceMetadata) {
        this.setSourceMetadata(playerSource, sourceMetadata)
    }

    override fun setSourceMetadata(playerSource: Source, sourceMetadata: SourceMetadata) {
        metadataProvider.setSourceMetadata(playerSource, sourceMetadata)
    }

    override fun getSourceMetadata(playerSource: Source): SourceMetadata {
        return metadataProvider.getSourceMetadata(playerSource) ?: SourceMetadata()
    }

    override fun setCustomData(playerSource: Source, customData: CustomData) {
        // we cannot put this logic into the adapter since the adapter is created on attaching
        // and this method might be called earlier
        val newActiveCustomData =
            ApiV3Utils.mergeCustomData(customData, metadataProvider.defaultMetadata.customData)
        val activeCustomDataChanged = analytics.activeCustomData != newActiveCustomData

        if (playerSource.isActive && activeCustomDataChanged) {
            analytics.closeCurrentSampleForCustomDataChangeIfNeeded()
        }

        val sourceMetadata = metadataProvider.getSourceMetadata(playerSource)
        if (sourceMetadata != null) {
            metadataProvider.setSourceMetadata(
                playerSource,
                sourceMetadata.copy(customData = customData),
            )
        } else {
            metadataProvider.setSourceMetadata(
                playerSource,
                SourceMetadata(customData = customData),
            )
        }
    }

    override fun getCustomData(playerSource: Source): CustomData {
        val currentSourceMetadata = metadataProvider.getSourceMetadata(playerSource)
        return currentSourceMetadata?.customData ?: CustomData()
    }
}
