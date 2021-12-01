package com.bitmovin.analytics.bitmovin.player

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.analytics.DefaultCollector
import com.bitmovin.analytics.adapters.PlayerAdapter
import com.bitmovin.analytics.bitmovin.player.features.BitmovinFeatureFactory
import com.bitmovin.analytics.features.FeatureFactory
import com.bitmovin.player.BitmovinPlayer

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
    constructor(bitmovinAnalyticsConfig: BitmovinAnalyticsConfig) : this(bitmovinAnalyticsConfig, bitmovinAnalyticsConfig.context) {
    }

    override fun createAdapter(bitmovinPlayer: BitmovinPlayer): PlayerAdapter {
        val featureFactory: FeatureFactory = BitmovinFeatureFactory(analytics, bitmovinPlayer)
        return BitmovinSdkAdapter(
            bitmovinPlayer,
            analytics.config,
            analytics.playerStateMachine,
            featureFactory,
            analytics.eventDataFactory,
            deviceInformationProvider
        )
    }

    companion object {
        private fun getUserAgent(context: Context): String {
            val applicationInfo = context.applicationInfo
            val stringId = applicationInfo.labelRes
            val applicationName = "Unknown"
            if (stringId == 0 && applicationInfo.nonLocalizedLabel != null) {
                applicationInfo.nonLocalizedLabel.toString()
            }
            val versionName: String = try {
                val packageName = context.packageName
                val info = context.packageManager.getPackageInfo(packageName, 0)
                info.versionName
            } catch (var5: PackageManager.NameNotFoundException) {
                "?"
            }
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
