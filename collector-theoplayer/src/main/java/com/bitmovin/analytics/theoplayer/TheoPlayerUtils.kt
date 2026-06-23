package com.bitmovin.analytics.theoplayer

import com.bitmovin.analytics.utils.Util
import com.theoplayer.android.api.event.ads.AdIntegrationKind

internal object TheoPlayerUtils {
    private const val GOOGLE_IMA_AD_CLASSNAME =
        "com.theoplayer.android.api.ads.ima.GoogleImaAd"

    private const val GOOGLE_IMA_ADBREAK_CLASSNAME =
        "com.theoplayer.android.api.ads.ima.GoogleImaAdBreak"

    val isTheoImaClassLoaded by lazy {
        Util.isClassLoaded(GOOGLE_IMA_AD_CLASSNAME, this.javaClass.classLoader)
    }

    val isTheoImaAdBreakClassLoaded by lazy {
        Util.isClassLoaded(GOOGLE_IMA_ADBREAK_CLASSNAME, this.javaClass.classLoader)
    }

    fun isClientSideAd(adIntegrationKind: AdIntegrationKind?): Boolean {
        // if adIntegrationKind is null we opt for CSAI, to keep current behaviour
        if (adIntegrationKind == null) {
            return true
        }

        // in case it is not Google_Ime we assume SSAI
        // This is following what the conviva integration does
        return adIntegrationKind == AdIntegrationKind.GOOGLE_IMA
    }
}
