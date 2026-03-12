package com.bitmovin.analytics.theoplayer.player

import com.bitmovin.analytics.enums.DRMType
import com.theoplayer.android.api.source.drm.DRMConfiguration

internal object PlayerUtils {
    fun getDrmTypeFromConfiguration(drmConfig: DRMConfiguration): String? {
        // Check for Widevine
        if (drmConfig.widevine != null) {
            return DRMType.WIDEVINE.value
        }

        // Check for PlayReady
        if (drmConfig.playready != null) {
            return DRMType.PLAYREADY.value
        }

        // Check for ClearKey
        if (drmConfig.clearkey != null) {
            return DRMType.CLEARKEY.value
        }

        // FairPlay is not tracked for Android
        return null
    }

    fun isPreRollAdOffset(timeOffset: String?): Boolean {
        if (timeOffset == null) {
            return false
        }

        if (timeOffset.equals("start", true)) {
            return true
        }

        if (timeOffset.equals("0", true)) {
            return true
        }

        return false
    }
}
