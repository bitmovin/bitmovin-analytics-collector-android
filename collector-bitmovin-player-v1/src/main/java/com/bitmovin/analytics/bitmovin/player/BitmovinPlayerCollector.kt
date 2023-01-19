package com.bitmovin.analytics.bitmovin.player

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.bitmovin.analytics.BitmovinAnalytics
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.bitmovin.player.features.BitmovinFeatureFactory
import com.bitmovin.analytics.data.DeviceInformationProvider
import com.bitmovin.analytics.data.EventDataFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.analytics.stateMachines.PlayerStateMachine
import com.bitmovin.player.BitmovinPlayer

@Deprecated(
    "Upgrade to v2 of com.bitmovin.analytics:collector-bitmovin-player"
)
class BitmovinPlayerCollector
/**
 * Bitmovin Analytics
 *
 * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
 * @param context [Context]
 */
    (bitmovinAnalyticsConfig: BitmovinAnalyticsConfig, context: Context) : DefaultCollector<BitmovinPlayer>(bitmovinAnalyticsConfig, context, getUserAgent(context)) {
    /**
     * Bitmovin Analytics
     *
     * @param bitmovinAnalyticsConfig [BitmovinAnalyticsConfig]
     */
    @Deprecated(
        """Please use {@link #BitmovinPlayerCollector(BitmovinAnalyticsConfig, Context)} and
          pass {@link Context} separately."""
    )
    constructor(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) : this(bitmovinAnalyticsConfig, bitmovinAnalyticsConfig.context ?: throw IllegalArgumentException("Context cannot be null")) {
    }

    override fun createAdapter(player: BitmovinPlayer, analytics: BitmovinAnalytics, stateMachine: PlayerStateMachine, deviceInformationProvider: DeviceInformationProvider, eventDataFactory: EventDataFactory): PlayerAdapter {
        val featureFactory: FeatureFactory = BitmovinFeatureFactory(analytics, player)
        return BitmovinSdkAdapter(
            player,
            config,
            stateMachine,
            featureFactory,
            eventDataFactory,
            deviceInformationProvider
        )
    }

    companion object {
        private fun getUserAgent(context: Context): String {
            val applicationInfo: ApplicationInfo? = context.applicationInfo
            val stringId = applicationInfo?.labelRes
            val applicationName = if (stringId == 0) applicationInfo?.nonLocalizedLabel?.toString() else null ?: "Unknown"
            val versionName = try {
                val packageName = context.packageName
                val info = context.packageManager?.getPackageInfo(packageName, 0)
                info?.versionName
            } catch (var5: PackageManager.NameNotFoundException) {
                null
            } ?: "?"
            return (applicationName +
                    "/" +
                    versionName +
                    " (Linux;Android " +
                    Build.VERSION.RELEASE +
                    ") " +
                    "BitmovinPlayer/" +
                    BitmovinUtil.getPlayerVersion())
        }
    }
}
