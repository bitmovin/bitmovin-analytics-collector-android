package com.bitmovin.analytics.bitmovin.player.providers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.bitmovin.player.api.PlayerConfig

/**
 * Provides the player_license in case it is not set through analytics config
 */
class PlayerLicenseProvider(private val context: Context) {
    fun getBitmovinPlayerLicenseKey(playerConfig: PlayerConfig): String? {
        if (!playerConfig.key.isNullOrBlank()) {
            return playerConfig.key
        }

        // we fall back to using the key from the manifest in case it is not specified by the customer
        // in the player config directly
        return getBitmovinLicenseKeyFromAppManifestOrNull()
    }

    // Workaround to retrieve PlayerKey from manifest (copied from player)
    private fun getBitmovinLicenseKeyFromAppManifestOrNull() = runCatching {
        val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getApplicationInfo(
                context.applicationContext.packageName,
                PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getApplicationInfo(
                context.applicationContext.packageName,
                PackageManager.GET_META_DATA,
            )
        }

        applicationInfo.metaData?.getString(BITMOVIN_PLAYER_LICENSE_KEY)
    }.getOrNull()

    companion object {
        private const val BITMOVIN_PLAYER_LICENSE_KEY = "BITMOVIN_PLAYER_LICENSE_KEY"
    }
}
