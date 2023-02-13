package com.bitmovin.analytics.bitmovin.player

import android.content.Context
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.bitmovin.player.features.BitmovinFeatureFactory
import com.bitmovin.analytics.bitmovin.player.providers.PlayerLicenseProvider
import com.bitmovin.analytics.config.SourceMetadata
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.analytics.utils.SystemInformationProvider
import com.bitmovin.analytics.utils.UserAgentProvider
import com.bitmovin.analytics.utils.Util
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.Source

class BitmovinPlayerCollector
/**
 * Bitmovin Analytics
 *
 * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
 * @param context [Context]
 */
(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig, private val context: Context) :
    DefaultCollector<Player>(bitmovinAnalyticsConfig, context) {
    private val sourceMetadataMap = HashMap<Source, SourceMetadata>()

    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
     */
    @Deprecated(
        """Please use {@link #BitmovinPlayerCollector(BitmovinAnalyticsConfig, Context)} and
          pass {@link Context} separately.""",
    )
    constructor(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) : this(
        bitmovinAnalyticsConfig,
        bitmovinAnalyticsConfig.context ?: throw IllegalArgumentException("Context cannot be null"),
    )

    override fun createAdapter(
        player: Player,
        analytics: BitmovinAnalytics,
        stateMachine: PlayerStateMachine,
    ): PlayerAdapter {
        val featureFactory: FeatureFactory = BitmovinFeatureFactory(analytics, player)
        val userAgentProvider = UserAgentProvider(
            Util.getApplicationInfoOrNull(context),
            Util.getPackageInfoOrNull(context),
            SystemInformationProvider.getProperty("http.agent"),
        )
        val eventDataFactory = EventDataFactory(config, userIdProvider, userAgentProvider)
        val deviceInformationProvider = DeviceInformationProvider(context)
        val playerLicenseProvider = PlayerLicenseProvider(context)
        return BitmovinSdkAdapter(
            player,
            config,
            stateMachine,
            featureFactory,
            sourceMetadataMap,
            eventDataFactory,
            deviceInformationProvider,
            playerLicenseProvider,
        )
    }

    fun addSourceMetadata(playerSource: Source, sourceMetadata: SourceMetadata) {
        sourceMetadataMap[playerSource] = sourceMetadata
    }
}
