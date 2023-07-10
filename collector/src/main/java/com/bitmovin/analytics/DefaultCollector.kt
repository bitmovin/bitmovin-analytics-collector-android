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
        // TODO: we might need to make sure this event is
        // handled by the main looper, since we might access to player from a different thread, which
        // could cause issues. (we should discuss thread safety in general with player folks)
        analytics.sendCustomDataEvent(customData)
    }

    override var defaultMetadata: DefaultMetadata
        get() = metadataProvider.defaultMetadata
        set(value) {
            metadataProvider.defaultMetadata = value
        }

    override fun setDefaultCustomData(customData: CustomData) {
        // TODO: we might need to make sure this event is
        // handled by the main looper, since we might access to player from a different thread, which
        // could cause issues. (we should discuss thread safety in general with player folks)
        analytics.closeCurrentSampleForCustomDataChangeIfNeeded()
        metadataProvider.defaultMetadata = metadataProvider.defaultMetadata.copy(customData = customData)
    }

    override fun getDefaultCustomData(): CustomData {
        return metadataProvider.defaultMetadata.customData
    }

    override fun setCurrentSourceCustomData(customData: CustomData) {
        // TODO: we might need to make sure this event is
        // handled by the main looper, since we might access to player from a different thread, which
        // could cause issues. (we should discuss thread safety in general with player folks)
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
        metadataProvider.setDeprectedBitmovinAnalyticsConfig(bitmovinAnalyticsConfig)
    }
}
