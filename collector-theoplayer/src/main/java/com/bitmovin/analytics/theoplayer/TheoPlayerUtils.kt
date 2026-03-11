package com.bitmovin.analytics.theoplayer

import com.bitmovin.analytics.utils.Util

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
}
