package com.bitmovin.analytics.bitmovin.player

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
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

/**
 * Bitmovin Analytics
 *
 * @param analyticsConfig [AnalyticsConfig]
 * @param context [Context]
 */

@Deprecated(
    "Please use {@link IBitmovinCollector.Factory instead} instead.",
    replaceWith = ReplaceWith(
        expression = "IBitmovinPlayerCollector.Factory.create(context, analyticsConfig)",
        imports = ["com.bitmovin.analytics.bitmovin.player.IBitmovinPlayerCollector"],
    ),
)
class BitmovinPlayerCollector(analyticsConfig: AnalyticsConfig, private val context: Context) :
    DefaultCollector<Player>(analyticsConfig, context), IBitmovinPlayerCollector {

    private var player: Player? = null

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
            imports = ["com.bitmovin.analytics.bitmovin.player.IBitmovinPlayerCollector"],
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
        // TODO: storing the player explicitly here is not nice, we should see how we can retrieve
        // the source without storing the player explicitly here
        this.player = player
        val featureFactory: FeatureFactory = BitmovinFeatureFactory(analytics, player)
        val userAgentProvider = UserAgentProvider(
            Util.getApplicationInfoOrNull(context),
            Util.getPackageInfoOrNull(context),
            SystemInformationProvider.getProperty("http.agent"),
        )
        val eventDataFactory = EventDataFactory(config, userIdProvider, userAgentProvider)
        val deviceInformationProvider = DeviceInformationProvider(context)
        val playerLicenseProvider = PlayerLicenseProvider(context)
        val playerContext = BitmovinPlayerContext(player)
        val stateMachine = PlayerStateMachine.Factory.create(analytics, playerContext)
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
        ReplaceWith("setSourceMetadata(playerSource, sourceMetadata)"),
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

    override fun setCurrentSourceCustomData(customData: CustomData) {
        // we cannot put this logic into the adapter since the adapter is created on attaching
        // and this method might be called earlier
        analytics.closeCurrentSampleForCustomDataChangeIfNeeded()
        val activeSource = this.player?.source
        if (activeSource != null) {
            val currentSourceMetadata = metadataProvider.getSourceMetadata(activeSource)
            if (currentSourceMetadata != null) {
                metadataProvider.setSourceMetadata(activeSource, currentSourceMetadata.copy(customData = customData))
                return
            }
        }

        val sourceMetadata = metadataProvider.getSourceMetadata()
        if (sourceMetadata != null) {
            metadataProvider.setSourceMetadata(sourceMetadata.copy(customData = customData))
            return
        }

        metadataProvider.setSourceMetadata(SourceMetadata(customData = customData))
        return
    }

    override fun setCurrentSourceMetadata(sourceMetadata: SourceMetadata) {
        val activeSource = this.player?.source
        if (activeSource != null) {
            val currentSourceMetadata = metadataProvider.getSourceMetadata(activeSource)
            if (currentSourceMetadata != null) {
                metadataProvider.setSourceMetadata(activeSource, sourceMetadata)
                return
            }
        }

        metadataProvider.setSourceMetadata(sourceMetadata)
    }

    // TODO: should we make it nullable or not?
    override fun getCurrentSourceMetadata(): SourceMetadata {
        val activeSource = this.player?.source
        if (activeSource != null) {
            val currentSourceMetadata = metadataProvider.getSourceMetadata(activeSource)
            if (currentSourceMetadata != null) {
                return currentSourceMetadata
            }
        }

        return metadataProvider.getSourceMetadata() ?: SourceMetadata()
    }
}
