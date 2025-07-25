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
import com.bitmovin.analytics.internal.InternalBitmovinApi
import com.bitmovin.analytics.utils.ApiV3Utils

@InternalBitmovinApi
abstract class DefaultCollector<TPlayer> protected constructor(
    val config: AnalyticsConfig,
    context: Context,
    protected val metadataProvider: MetadataProvider = MetadataProvider(),
) : AnalyticsCollector<TPlayer> {
    // TODO[AN-3692]: why is this lazy and not part of the constructor for easier testing?
    protected open val analytics by lazy {
        BitmovinAnalytics(config, context)
    }

    protected val userIdProvider: UserIdProvider =
        if (config.randomizeUserId) {
            RandomizedUserIdIdProvider()
        } else {
            SecureSettingsAndroidIdUserIdProvider(
                context,
            )
        }

    override val impressionId: String?
        get() {
            // we are only returning a valid impressionId if the player is already attached
            // this is needed to mimic the existing behaviour and since we are
            // resetting the impressionId on attaching of the collector, which would mean that
            // the user gets a different impressionId before attaching
            return if (analytics.isAttachedToPlayer()) {
                analytics.impressionId
            } else {
                null
            }
        }

    override val userId: String
        get() = userIdProvider.userId()

    override var defaultMetadata: DefaultMetadata
        get() = metadataProvider.defaultMetadata
        set(value) {
            metadataProvider.defaultMetadata = value
        }

    var customData: CustomData
        get() = metadataProvider.getSourceMetadata()?.customData ?: CustomData()
        set(value) {
            val newActiveCustomData = ApiV3Utils.mergeCustomData(value, metadataProvider.defaultMetadata.customData)
            if (newActiveCustomData != analytics.activeCustomData) {
                analytics.closeCurrentSampleForCustomDataChangeIfNeeded()
            }

            // for backwards compatibility we set customData on the deprecated bitmovinAnalyticsConfig
            // if deprecated config is used and no sourceMetadata is set
            // this can be removed once deprecated config is removed
            if (!metadataProvider.sourceMetadataIsSet() &&
                metadataProvider.deprecatedBitmovinAnalyticsConfigIsSet()
            ) {
                setCustomDataOnDeprecatedBitmovinConfig(value)
                return
            }

            val newSourceMetadata = metadataProvider.getSourceMetadata()?.copy(customData = value) ?: SourceMetadata(customData = value)
            metadataProvider.setSourceMetadata(newSourceMetadata)
        }

    protected abstract fun createAdapter(
        player: TPlayer,
        analytics: BitmovinAnalytics,
    ): PlayerAdapter

    override fun attachPlayer(player: TPlayer) {
        analytics.detachPlayer()
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
        // TODO[AN-3684]: we might need to make sure this event is
        // handled by the main looper, since we might access to player from a different thread, which
        // could cause issues. (we should discuss thread safety in general with player folks)
        analytics.sendCustomDataEvent(customData)
    }

    fun setDeprecatedBitmovinAnalyticsConfig(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) {
        metadataProvider.setDeprecatedBitmovinAnalyticsConfig(bitmovinAnalyticsConfig)
    }

    private fun setCustomDataOnDeprecatedBitmovinConfig(customData: CustomData) {
        val deprecatedBitmovinAnalyticsConfig = metadataProvider.getDeprecatedBitmovinAnalyticsConfig()
        deprecatedBitmovinAnalyticsConfig?.customData1 = customData.customData1
        deprecatedBitmovinAnalyticsConfig?.customData2 = customData.customData2
        deprecatedBitmovinAnalyticsConfig?.customData3 = customData.customData3
        deprecatedBitmovinAnalyticsConfig?.customData4 = customData.customData4
        deprecatedBitmovinAnalyticsConfig?.customData5 = customData.customData5
        deprecatedBitmovinAnalyticsConfig?.customData6 = customData.customData6
        deprecatedBitmovinAnalyticsConfig?.customData7 = customData.customData7
        deprecatedBitmovinAnalyticsConfig?.customData8 = customData.customData8
        deprecatedBitmovinAnalyticsConfig?.customData9 = customData.customData9
        deprecatedBitmovinAnalyticsConfig?.customData10 = customData.customData10
        deprecatedBitmovinAnalyticsConfig?.customData11 = customData.customData11
        deprecatedBitmovinAnalyticsConfig?.customData12 = customData.customData12
        deprecatedBitmovinAnalyticsConfig?.customData13 = customData.customData13
        deprecatedBitmovinAnalyticsConfig?.customData14 = customData.customData14
        deprecatedBitmovinAnalyticsConfig?.customData15 = customData.customData15
        deprecatedBitmovinAnalyticsConfig?.customData16 = customData.customData16
        deprecatedBitmovinAnalyticsConfig?.customData17 = customData.customData17
        deprecatedBitmovinAnalyticsConfig?.customData18 = customData.customData18
        deprecatedBitmovinAnalyticsConfig?.customData19 = customData.customData19
        deprecatedBitmovinAnalyticsConfig?.customData20 = customData.customData20
        deprecatedBitmovinAnalyticsConfig?.customData21 = customData.customData21
        deprecatedBitmovinAnalyticsConfig?.customData22 = customData.customData22
        deprecatedBitmovinAnalyticsConfig?.customData23 = customData.customData23
        deprecatedBitmovinAnalyticsConfig?.customData24 = customData.customData24
        deprecatedBitmovinAnalyticsConfig?.customData25 = customData.customData25
        deprecatedBitmovinAnalyticsConfig?.customData26 = customData.customData26
        deprecatedBitmovinAnalyticsConfig?.customData27 = customData.customData27
        deprecatedBitmovinAnalyticsConfig?.customData28 = customData.customData28
        deprecatedBitmovinAnalyticsConfig?.customData29 = customData.customData29
        deprecatedBitmovinAnalyticsConfig?.customData30 = customData.customData30
        deprecatedBitmovinAnalyticsConfig?.experimentName = customData.experimentName
        deprecatedBitmovinAnalyticsConfig?.customData31 = customData.customData31
        deprecatedBitmovinAnalyticsConfig?.customData32 = customData.customData32
        deprecatedBitmovinAnalyticsConfig?.customData33 = customData.customData33
        deprecatedBitmovinAnalyticsConfig?.customData34 = customData.customData34
        deprecatedBitmovinAnalyticsConfig?.customData35 = customData.customData35
        deprecatedBitmovinAnalyticsConfig?.customData36 = customData.customData36
        deprecatedBitmovinAnalyticsConfig?.customData37 = customData.customData37
        deprecatedBitmovinAnalyticsConfig?.customData38 = customData.customData38
        deprecatedBitmovinAnalyticsConfig?.customData39 = customData.customData39
        deprecatedBitmovinAnalyticsConfig?.customData40 = customData.customData40
        deprecatedBitmovinAnalyticsConfig?.customData41 = customData.customData41
        deprecatedBitmovinAnalyticsConfig?.customData42 = customData.customData42
        deprecatedBitmovinAnalyticsConfig?.customData43 = customData.customData43
        deprecatedBitmovinAnalyticsConfig?.customData44 = customData.customData44
        deprecatedBitmovinAnalyticsConfig?.customData45 = customData.customData45
        deprecatedBitmovinAnalyticsConfig?.customData46 = customData.customData46
        deprecatedBitmovinAnalyticsConfig?.customData47 = customData.customData47
        deprecatedBitmovinAnalyticsConfig?.customData48 = customData.customData48
        deprecatedBitmovinAnalyticsConfig?.customData49 = customData.customData49
        deprecatedBitmovinAnalyticsConfig?.customData50 = customData.customData50
    }
}
