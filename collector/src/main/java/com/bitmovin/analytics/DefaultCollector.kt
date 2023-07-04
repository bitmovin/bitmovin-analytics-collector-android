package com.bitmovin.analytics

import android.content.Context
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.api.AnalyticsCollector
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.DefaultMetadata
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.analytics.data.MetadataProvider
import com.bitmovin.analytics.data.RandomizedUserIdIdProvider
import com.bitmovin.analytics.data.SecureSettingsAndroidIdUserIdProvider
import com.bitmovin.analytics.data.UserIdProvider
import com.bitmovin.analytics.utils.Util

// TODO: we should probably move this class into API package
abstract class DefaultCollector<TPlayer> protected constructor(
    final override val config: AnalyticsConfig,
    context: Context,
) : AnalyticsCollector<TPlayer> {
    protected val analytics by lazy { BitmovinAnalytics(config, context) }
    protected val metadataProvider = MetadataProvider()

    protected val userIdProvider: UserIdProvider =
        if (config.randomizeUserId) {
            RandomizedUserIdIdProvider()
        } else {
            SecureSettingsAndroidIdUserIdProvider(
                context,
            )
        }

    override val impressionId: String?
        get() = analytics.impressionId

    override val version: String
        get() = Util.analyticsVersion

    override val userId: String
        get() = userIdProvider.userId()

    override val customData: CustomData
        get() = analytics.customData

    protected abstract fun createAdapter(
        player: TPlayer,
        analytics: BitmovinAnalytics,
    ): PlayerAdapter

    override fun attachPlayer(player: TPlayer) {
        val adapter = createAdapter(player, analytics)
        analytics.attach(adapter)
    }

    override fun detachPlayer() {
        analytics.detachPlayer()
    }

    @Deprecated(
        """Please use {@link #sendCustomDataEvent(CustomData)} instead.""",
        ReplaceWith("sendCustomDataEvent(customData)"),
    )
    override fun setCustomDataOnce(customData: CustomData) {
        analytics.sendCustomDataEvent(customData)
    }

    override fun sendCustomDataEvent(customData: CustomData) {
        analytics.sendCustomDataEvent(customData)
    }

    override var defaultMetadata: DefaultMetadata
        get() = metadataProvider.defaultMetadata
        set(value) {
            metadataProvider.defaultMetadata = value
        }

    override fun setDefaultCustomData(customData: CustomData) {
        analytics.closeCurrentSampleForCustomDataChangeIfNeeded()
        metadataProvider.defaultMetadata = metadataProvider.defaultMetadata.copy(customData = customData)
    }

    override fun getDefaultCustomData(): CustomData {
        return metadataProvider.defaultMetadata.customData
    }

    override fun setCurrentSourceCustomData(customData: CustomData) {
        analytics.closeCurrentSampleForCustomDataChangeIfNeeded()
        this.setCurrentSourceMetadata(this.getCurrentSourceMetadata().copy(customData = customData))
    }

    override fun getCurrentSourceCustomData(): CustomData {
        return metadataProvider.getSourceMetadata()?.customData ?: CustomData()
    }

    override fun setCurrentSourceMetadata(sourceMetadata: SourceMetadata) {
        metadataProvider.setSourceMetadata(sourceMetadata)
    }

    override fun getCurrentSourceMetadata(): SourceMetadata {
        return metadataProvider.getSourceMetadata() ?: SourceMetadata()
    }

    fun setDeprecatedBitmovinAnalyticsConfig(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) {
        metadataProvider.deprecatedBitmovinAnalyticsConfig = bitmovinAnalyticsConfig
    }
}
