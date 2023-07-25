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
import com.bitmovin.analytics.utils.ApiV3Utils

abstract class DefaultCollector<TPlayer> protected constructor(
    val config: AnalyticsConfig,
    context: Context,
    protected val metadataProvider: MetadataProvider = MetadataProvider(),
) : AnalyticsCollector<TPlayer> {

    // TODO: why is this lazy and not port of the constructor for easier testing?
    protected open val analytics by lazy { BitmovinAnalytics(config, context) }

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

    override val userId: String
        get() = userIdProvider.userId()

    var defaultMetadata: DefaultMetadata
        get() = metadataProvider.defaultMetadata
        set(value) {
            metadataProvider.defaultMetadata = value
        }

    var customData: CustomData
        get() = metadataProvider.getSourceMetadata()?.customData ?: CustomData()
        set(newCustomData) {

            val newActiveCustomData = ApiV3Utils.mergeCustomData(newCustomData, metadataProvider.defaultMetadata.customData)
            if (newActiveCustomData != analytics.customData) {
                analytics.closeCurrentSampleForCustomDataChangeIfNeeded()
            }

            val newSourceMetadata = metadataProvider.getSourceMetadata()?.copy(customData = newCustomData) ?: SourceMetadata(customData = newCustomData)
            metadataProvider.setSourceMetadata(newSourceMetadata)
        }

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
        this.sendCustomDataEvent(customData)
    }

    override fun sendCustomDataEvent(customData: CustomData) {
        // TODO: we might need to make sure this event is
        // handled by the main looper, since we might access to player from a different thread, which
        // could cause issues. (we should discuss thread safety in general with player folks)
        analytics.sendCustomDataEvent(customData)
    }

    fun setDeprecatedBitmovinAnalyticsConfig(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) {
        metadataProvider.setDeprectedBitmovinAnalyticsConfig(bitmovinAnalyticsConfig)
    }
}
