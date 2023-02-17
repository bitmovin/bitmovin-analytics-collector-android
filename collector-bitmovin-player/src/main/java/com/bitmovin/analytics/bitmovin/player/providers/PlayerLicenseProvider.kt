package com.bitmovin.analytics.bitmovin.player.providers

import android.content.Context
import com.bitmovin.analytics.utils.Util.getApplicationInfoOrNull
import com.bitmovin.player.api.PlayerConfig

/**
 * Provides the player_license in case it is not set through analytics config
 */
internal class PlayerLicenseProvider(private val context: Context) {
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
        val applicationInfo = getApplicationInfoOrNull(context)
        applicationInfo?.metaData?.getString(BITMOVIN_PLAYER_LICENSE_KEY)
    }.getOrNull()

    companion object {
        private const val BITMOVIN_PLAYER_LICENSE_KEY = "BITMOVIN_PLAYER_LICENSE_KEY"
    }
}
