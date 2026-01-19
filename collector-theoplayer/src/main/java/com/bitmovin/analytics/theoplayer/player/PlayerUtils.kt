package com.bitmovin.analytics.theoplayer.player

import com.bitmovin.analytics.enums.DRMType
import com.theoplayer.android.api.source.drm.DRMConfiguration

public object PlayerUtils {
    public fun getDrmTypeFromConfiguration(drmConfig: DRMConfiguration): String? {
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
}
